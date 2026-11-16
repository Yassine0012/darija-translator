// Service worker: wires the right-click menu and the toolbar icon to the side panel.

const MENU_ID = "translate-darija";

chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.create({
    id: MENU_ID,
    title: "Translate to Darija",
    contexts: ["selection"],
  });
});

// Right-click on selected text -> open the side panel and hand it the text.
chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (info.menuItemId !== MENU_ID || !tab) return;
  // sidePanel.open() must run synchronously inside the user gesture: keep it first.
  chrome.sidePanel.open({ tabId: tab.id });
  chrome.storage.session.set({
    pending: { text: info.selectionText || "", ts: Date.now() },
  });
});

// Toolbar icon -> open the side panel and copy the page's current selection into it.
chrome.action.onClicked.addListener(async (tab) => {
  chrome.sidePanel.open({ tabId: tab.id });
  let text = "";
  try {
    const [result] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: () => window.getSelection().toString(),
    });
    text = (result && result.result) || "";
  } catch (e) {
    // Restricted pages (chrome://, Web Store...) cannot be read: open with an empty box.
  }
  await chrome.storage.session.set({ pending: { text, ts: Date.now() } });
});
