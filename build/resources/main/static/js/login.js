const form = document.getElementById("login-form");
const statusBox = document.getElementById("login-status");

function setStatus(message, error = false) {
  statusBox.textContent = message;
  statusBox.className = error ? "status error" : "status ok";
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();

  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value;

  setStatus("Signing in...");

  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ username, password })
    });

    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload.error || "Login failed");
    }

    window.location.href = "/app";
  } catch (error) {
    setStatus(error.message, true);
  }
});
