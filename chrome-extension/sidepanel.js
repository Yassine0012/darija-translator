const $ = (id) => document.getElementById(id);

const DEFAULTS = {
  url: "http://localhost:8080/api/translate",
  user: "admin",
  password: "",
};

let lastHandled = 0;

function setStatus(message, isError = false) {
  const el = $("status");
  el.textContent = message;
  el.classList.toggle("error", isError);
}

// UTF-8-safe Base64 for the Basic auth header.
function basicAuth(user, password) {
  const bytes = new TextEncoder().encode(`${user}:${password}`);
  return "Basic " + btoa(String.fromCharCode(...bytes));
}

// ---------- Settings ----------
async function loadSettings() {
  const s = await chrome.storage.local.get(DEFAULTS);
  $("url").value = s.url;
  $("user").value = s.user;
  $("password").value = s.password;
  if (!s.password) $("settings").open = true; // first run: ask for credentials
}

$("save").addEventListener("click", async () => {
  await chrome.storage.local.set({
    url: $("url").value.trim(),
    user: $("user").value.trim(),
    password: $("password").value,
  });
  setStatus("Settings saved.");
});

// ---------- Translation ----------
async function translate() {
  const text = $("input").value.trim();
  if (!text) return;

  const s = await chrome.storage.local.get(DEFAULTS);
  $("translate").disabled = true;
  $("output").textContent = "";
  setStatus("Translating…");

  try {
    const response = await fetch(s.url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: basicAuth(s.user, s.password),
      },
      body: JSON.stringify({ text, source: $("source").value }),
    });

    if (response.status === 401) {
      $("settings").open = true;
      throw new Error("Authentication failed: check username and password in Settings.");
    }
    if (!response.ok) {
      throw new Error(`Server error (HTTP ${response.status}).`);
    }
    const data = await response.json();
    $("output").textContent = data.translation;
    setStatus("");
  } catch (e) {
    const unreachable = e instanceof TypeError;
    setStatus(unreachable ? "Cannot reach the server. Is it running at the URL in Settings?" : e.message, true);
  } finally {
    $("translate").disabled = false;
  }
}

$("translate").addEventListener("click", translate);
$("input").addEventListener("keydown", (e) => {
  if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) translate();
});
$("clear").addEventListener("click", () => {
  $("input").value = "";
  $("output").textContent = "";
  setStatus("");
  $("input").focus();
});

// ---------- Extras: copy and read aloud ----------
$("copy").addEventListener("click", async () => {
  const text = $("output").textContent;
  if (!text) return;
  await navigator.clipboard.writeText(text);
  setStatus("Copied.");
});

$("speak").addEventListener("click", () => {
  const text = $("output").textContent;
  if (!text) return;
  speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  const voice = speechSynthesis.getVoices().find((v) => v.lang.toLowerCase().startsWith("ar"));
  if (voice) {
    utterance.voice = voice;
    utterance.lang = voice.lang;
  } else {
    utterance.lang = "ar";
    setStatus("No Arabic voice found on this system; using the default voice.");
  }
  speechSynthesis.speak(utterance);
});

// ---------- Text coming from the page (right-click menu / toolbar icon) ----------
async function consumePending() {
  const { pending } = await chrome.storage.session.get("pending");
  if (!pending || pending.ts <= lastHandled) return;
  if (Date.now() - pending.ts > 5000) return; // ignore stale requests
  lastHandled = pending.ts;
  if (!pending.text.trim()) {
    $("input").focus();
    return;
  }
  $("input").value = pending.text;
  translate();
}

chrome.storage.onChanged.addListener((changes, area) => {
  if (area === "session" && changes.pending) consumePending();
});

loadSettings().then(consumePending);
