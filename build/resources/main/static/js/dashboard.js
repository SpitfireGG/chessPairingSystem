const sessionUser = document.getElementById("session-user");
const playerForm = document.getElementById("player-form");
const playersBody = document.getElementById("players-body");
const tournamentsBody = document.getElementById("tournaments-body");
const tournamentForm = document.getElementById("tournament-form");
const tournamentSelect = document.getElementById("tournament-select");
const matchesBody = document.getElementById("matches-body");
const resultForm = document.getElementById("result-form");
const resultMatchSelect = document.getElementById("result-match");
const leaderboardBody = document.getElementById("leaderboard-body");

let selectedTournamentId = null;
let currentMatches = [];

function setStatus(id, message, error = false) {
  const el = document.getElementById(id);
  el.textContent = message;
  el.className = error ? "status error" : "status ok";
}

async function apiRequest(path, options = {}) {
  const response = await fetch(path, options);
  let payload = {};
  try {
    payload = await response.json();
  } catch (_) {
    payload = {};
  }

  if (!response.ok) {
    throw new Error(payload.error || `Request failed (${response.status})`);
  }

  return payload;
}

async function checkSession() {
  try {
    const payload = await apiRequest("/api/admin/session");
    sessionUser.textContent = `Logged in as ${payload.username}`;
  } catch (_) {
    window.location.href = "/login";
  }
}

async function loadPlayers() {
  try {
    const payload = await apiRequest("/api/admin/players");
    playersBody.innerHTML = "";

    payload.players.forEach((player) => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${player.id}</td>
        <td>${player.name}</td>
        <td>${player.initialRank}</td>
        <td>${player.currentScore.toFixed(1)}</td>
        <td><button class="btn danger" data-player-id="${player.id}">Delete</button></td>
      `;
      playersBody.appendChild(row);
    });

    setStatus("players-status", `Loaded ${payload.players.length} players.`);
  } catch (error) {
    setStatus("players-status", error.message, true);
  }
}

async function loadTournaments() {
  try {
    const payload = await apiRequest("/api/admin/tournaments");
    tournamentsBody.innerHTML = "";
    tournamentSelect.innerHTML = "";

    payload.tournaments.forEach((tournament) => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${tournament.id}</td>
        <td>${tournament.name}</td>
        <td>${tournament.currentRound}</td>
        <td>${tournament.status}</td>
      `;
      tournamentsBody.appendChild(row);

      const option = document.createElement("option");
      option.value = String(tournament.id);
      option.textContent = `#${tournament.id} - ${tournament.name}`;
      tournamentSelect.appendChild(option);
    });

    if (payload.tournaments.length > 0) {
      if (!selectedTournamentId) {
        selectedTournamentId = payload.tournaments[0].id;
      }
      tournamentSelect.value = String(selectedTournamentId);
      setStatus("tournaments-status", `Loaded ${payload.tournaments.length} tournaments.`);
    } else {
      selectedTournamentId = null;
      setStatus("tournaments-status", "No tournaments yet.");
    }
  } catch (error) {
    setStatus("tournaments-status", error.message, true);
  }
}

function renderMatches(payload) {
  currentMatches = payload.matches || [];
  matchesBody.innerHTML = "";
  resultMatchSelect.innerHTML = "";

  currentMatches.forEach((match) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${match.tableNumber}</td>
      <td>${match.player1Name}</td>
      <td>${match.player2Name ?? "BYE"}</td>
      <td>${match.result ?? "PENDING"}</td>
    `;
    matchesBody.appendChild(row);

    const option = document.createElement("option");
    option.value = String(match.matchId);
    option.textContent = `Table ${match.tableNumber}: ${match.player1Name} vs ${match.player2Name ?? "BYE"}`;
    resultMatchSelect.appendChild(option);
  });

  if (currentMatches.length === 0) {
    setStatus("pairings-status", "No pairings yet for selected tournament.");
  } else {
    setStatus("pairings-status", `Round ${payload.roundNumber} has ${currentMatches.length} matches.`);
  }
}

async function loadMatches() {
  if (!selectedTournamentId) {
    matchesBody.innerHTML = "";
    resultMatchSelect.innerHTML = "";
    setStatus("pairings-status", "Select or create a tournament.");
    return;
  }

  try {
    const payload = await apiRequest(`/api/admin/tournaments/${selectedTournamentId}/matches?round=current`);
    renderMatches(payload);
  } catch (error) {
    setStatus("pairings-status", error.message, true);
  }
}

async function loadLeaderboard() {
  if (!selectedTournamentId) {
    leaderboardBody.innerHTML = "";
    setStatus("leaderboard-status", "Select or create a tournament.");
    return;
  }

  try {
    const payload = await apiRequest(`/api/admin/tournaments/${selectedTournamentId}/leaderboard`);
    leaderboardBody.innerHTML = "";

    payload.entries.forEach((entry) => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${entry.rank}</td>
        <td>${entry.playerName}</td>
        <td>${entry.score.toFixed(1)}</td>
        <td>${entry.initialRank}</td>
      `;
      leaderboardBody.appendChild(row);
    });

    setStatus("leaderboard-status", `Leaderboard updated (${payload.entries.length} rows).`);
  } catch (error) {
    setStatus("leaderboard-status", error.message, true);
  }
}

playerForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const name = document.getElementById("player-name").value.trim();
  const initialRank = document.getElementById("player-rank").value;

  try {
    await apiRequest("/api/admin/players", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ name, initialRank })
    });
    playerForm.reset();
    await loadPlayers();
  } catch (error) {
    setStatus("players-status", error.message, true);
  }
});

playersBody.addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-player-id]");
  if (!button) {
    return;
  }

  try {
    await apiRequest(`/api/admin/players/${button.dataset.playerId}`, {
      method: "DELETE"
    });
    await loadPlayers();
  } catch (error) {
    setStatus("players-status", error.message, true);
  }
});

tournamentForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const name = document.getElementById("tournament-name").value.trim();

  try {
    const payload = await apiRequest("/api/admin/tournaments", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ name })
    });
    selectedTournamentId = payload.tournamentId;
    tournamentForm.reset();
    await loadTournaments();
    await loadMatches();
    await loadLeaderboard();
  } catch (error) {
    setStatus("tournaments-status", error.message, true);
  }
});

tournamentSelect.addEventListener("change", async (event) => {
  selectedTournamentId = Number(event.target.value);
  await loadMatches();
  await loadLeaderboard();
});

document.getElementById("generate-pairings-btn").addEventListener("click", async () => {
  if (!selectedTournamentId) {
    setStatus("pairings-status", "Select a tournament first.", true);
    return;
  }

  try {
    const payload = await apiRequest(`/api/admin/tournaments/${selectedTournamentId}/pairings/generate`, {
      method: "POST"
    });
    renderMatches(payload);
    await loadLeaderboard();
  } catch (error) {
    setStatus("pairings-status", error.message, true);
  }
});

resultForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  const matchId = resultMatchSelect.value;
  const result = document.getElementById("result-value").value;

  if (!matchId) {
    setStatus("results-status", "No match available to update.", true);
    return;
  }

  try {
    await apiRequest(`/api/admin/matches/${matchId}/result`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ result })
    });

    setStatus("results-status", "Result saved.");
    await loadPlayers();
    await loadMatches();
    await loadLeaderboard();
  } catch (error) {
    setStatus("results-status", error.message, true);
  }
});

document.getElementById("refresh-leaderboard-btn").addEventListener("click", loadLeaderboard);

document.getElementById("logout-btn").addEventListener("click", async () => {
  await fetch("/api/auth/logout", { method: "POST" });
  window.location.href = "/login";
});

(async () => {
  await checkSession();
  await loadPlayers();
  await loadTournaments();
  await loadMatches();
  await loadLeaderboard();
})();
