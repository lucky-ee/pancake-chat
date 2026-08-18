// ============================================================
// Java Chat — test client
// Talks to the REST API for auth/rooms/history, and to the
// STOMP/WebSocket endpoint for live messages.
// ============================================================

const API_BASE = ''; // same origin; set e.g. 'http://localhost:8080' if served separately
let currentUser = null;   // { userId, username }
let authToken = null;     // bearer token issued at login/register
let currentRoom = null;   // { id, name }
let stompClient = null;
let roomSubscription = null;

// ---------- element refs ----------
const screens = {
  auth: document.getElementById('auth-screen'),
  rooms: document.getElementById('rooms-screen'),
  chat: document.getElementById('chat-screen'),
};

function showScreen(name) {
  Object.values(screens).forEach(s => s.classList.add('hidden'));
  screens[name].classList.remove('hidden');
}

// ---------- helpers ----------
async function apiFetch(path, options = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

  const res = await fetch(API_BASE + path, {
    headers,
    ...options,
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.message || `Request failed (${res.status})`);
  }
  return data;
}

function formatTime(iso) {
  const d = new Date(iso);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// ============================================================
// AUTH
// ============================================================
document.getElementById('login-btn').onclick = () => doAuth('/api/auth/login');
document.getElementById('register-btn').onclick = () => doAuth('/api/auth/register');

async function doAuth(path) {
  const username = document.getElementById('auth-username').value.trim();
  const password = document.getElementById('auth-password').value;
  const errorEl = document.getElementById('auth-error');
  errorEl.textContent = '';

  if (!username || !password) {
    errorEl.textContent = 'Enter a username and password.';
    return;
  }

  try {
    const data = await apiFetch(path, {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    currentUser = { userId: data.userId, username: data.username };
    authToken = data.token;
    document.getElementById('whoami').textContent = `~ ${currentUser.username}`;
    showScreen('rooms');
    loadRooms();
  } catch (err) {
    errorEl.textContent = err.message;
  }
}

document.getElementById('logout-btn').onclick = async () => {
  disconnectSocket();
  try {
    await apiFetch('/api/auth/logout', { method: 'POST' });
  } catch (_) {
    // best-effort — proceed with local logout regardless
  }
  currentUser = null;
  authToken = null;
  document.getElementById('auth-username').value = '';
  document.getElementById('auth-password').value = '';
  showScreen('auth');
};

// ============================================================
// ROOMS
// ============================================================
async function loadRooms() {
  const listEl = document.getElementById('room-list');
  const errorEl = document.getElementById('rooms-error');
  errorEl.textContent = '';
  listEl.innerHTML = '<li class="empty">Loading…</li>';

  try {
    const rooms = await apiFetch('/api/rooms');
    listEl.innerHTML = '';
    if (rooms.length === 0) {
      listEl.innerHTML = '<li class="empty">No rooms yet — create one above.</li>';
      return;
    }
    rooms.forEach(room => {
      const li = document.createElement('li');
      li.textContent = room.name;
      li.onclick = () => enterRoom(room);
      listEl.appendChild(li);
    });
  } catch (err) {
    errorEl.textContent = err.message;
    listEl.innerHTML = '';
  }
}

document.getElementById('create-room-btn').onclick = async () => {
  const input = document.getElementById('new-room-name');
  const name = input.value.trim();
  const errorEl = document.getElementById('rooms-error');
  errorEl.textContent = '';
  if (!name) return;

  try {
    await apiFetch('/api/rooms', {
      method: 'POST',
      body: JSON.stringify({ name }),
    });
    input.value = '';
    loadRooms();
  } catch (err) {
    errorEl.textContent = err.message;
  }
};

document.getElementById('back-btn').onclick = () => {
  disconnectSocket();
  showScreen('rooms');
  loadRooms();
};

// ============================================================
// CHAT (REST history + STOMP live feed)
// ============================================================
async function enterRoom(room) {
  currentRoom = room;
  document.getElementById('chat-room-name').textContent = room.name;
  document.getElementById('messages').innerHTML = '';
  showScreen('chat');

  await loadHistory(room.id);
  connectSocket(room.id);
}

async function loadHistory(roomId) {
  try {
    const history = await apiFetch(`/api/rooms/${roomId}/messages?page=0&size=50`);
    history.forEach(renderMessage);
    scrollToBottom();
  } catch (err) {
    renderSystemLine(`Could not load history: ${err.message}`);
  }
}

function connectSocket(roomId) {
  // Token travels as a query param on the handshake URL — the server
  // validates it in AuthHandshakeInterceptor before the socket ever opens.
  const socket = new SockJS(API_BASE + '/ws?token=' + encodeURIComponent(authToken));
  stompClient = Stomp.over(socket);
  stompClient.debug = null; // silence verbose STOMP frame logging

  const statusDot = document.getElementById('chat-status');

  stompClient.connect({}, () => {
    statusDot.classList.add('connected');

    roomSubscription = stompClient.subscribe(`/topic/room/${roomId}`, (frame) => {
      const message = JSON.parse(frame.body);
      renderMessage(message);
      scrollToBottom();
    });

    // Announce presence
    stompClient.send(`/app/chat.join/${roomId}`, {}, JSON.stringify({
      sender: currentUser.username,
      content: '',
    }));
  }, () => {
    statusDot.classList.remove('connected');
    renderSystemLine('Connection lost. Go back and re-enter the room to retry.');
  });
}

function disconnectSocket() {
  if (roomSubscription) { roomSubscription.unsubscribe(); roomSubscription = null; }
  if (stompClient && stompClient.connected) { stompClient.disconnect(); }
  stompClient = null;
  document.getElementById('chat-status').classList.remove('connected');
}

document.getElementById('message-form').onsubmit = (e) => {
  e.preventDefault();
  const input = document.getElementById('message-input');
  const content = input.value.trim();
  if (!content || !stompClient || !stompClient.connected) return;

  stompClient.send(`/app/chat.sendMessage/${currentRoom.id}`, {}, JSON.stringify({
    sender: currentUser.username,
    content,
  }));
  input.value = '';
};

// ---------- rendering ----------
function renderMessage(msg) {
  const container = document.getElementById('messages');
  const line = document.createElement('div');
  line.className = 'msg-line';

  if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
    line.innerHTML = `<span class="msg-time">${formatTime(msg.createdAt)}</span>` +
      `<span class="msg-system">${escapeHtml(msg.content)}</span>`;
  } else {
    const isMe = currentUser && msg.sender === currentUser.username;
    line.innerHTML =
      `<span class="msg-time">${formatTime(msg.createdAt)}</span>` +
      `<span class="msg-sender ${isMe ? 'me' : 'other'}">&lt;${escapeHtml(msg.sender)}&gt;</span>` +
      `<span class="msg-content">${escapeHtml(msg.content)}</span>`;
  }
  container.appendChild(line);
}

function renderSystemLine(text) {
  const container = document.getElementById('messages');
  const line = document.createElement('div');
  line.className = 'msg-line';
  line.innerHTML = `<span class="msg-system">* ${escapeHtml(text)}</span>`;
  container.appendChild(line);
  scrollToBottom();
}

function scrollToBottom() {
  const container = document.getElementById('messages');
  container.scrollTop = container.scrollHeight;
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
