(() => {
  const STORAGE_KEY = "cps-theme";
  const root = document.documentElement;

  function isValidTheme(value) {
    return value === "light" || value === "dark";
  }

  function preferredTheme() {
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light";
  }

  function getSavedTheme() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return isValidTheme(saved) ? saved : null;
    } catch (_) {
      return null;
    }
  }

  function setTheme(theme) {
    root.setAttribute("data-theme", theme);
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      button.textContent = theme === "dark" ? "Switch To Light" : "Switch To Dark";
      button.setAttribute("aria-label", button.textContent);
    });
  }

  function persistTheme(theme) {
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch (_) {
      // Ignore persistence failures (private mode, blocked storage, etc.)
    }
  }

  const initialTheme = getSavedTheme() ?? preferredTheme();
  setTheme(initialTheme);

  document.addEventListener("DOMContentLoaded", () => {
    setTheme(initialTheme);

    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      button.addEventListener("click", () => {
        const current = root.getAttribute("data-theme") === "dark" ? "dark" : "light";
        const next = current === "dark" ? "light" : "dark";
        setTheme(next);
        persistTheme(next);
      });
    });
  });
})();
