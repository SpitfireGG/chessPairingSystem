const tournamentSelect = document.getElementById("live-tournament-select");
const matchesBody = document.getElementById("live-matches-body");
const leaderboardBody = document.getElementById("live-leaderboard-body");
const statusEl = document.getElementById("live-status");

function setStatus(message, error = false) {
  statusEl.textContent = message;
  statusEl.className = error ? "status error" : "status ok";
}

async function apiRequest(path) {
  const response = await fetch(path);
  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload.error || "Request failed");
  }
  return payload;
}

async function loadTournaments() {
  const payload = await apiRequest("/api/public/tournaments");
  tournamentSelect.innerHTML = "";

  payload.tournaments.forEach((tournament) => {
    const option = document.createElement("option");
    option.value = String(tournament.id);
    option.textContent = `#${tournament.id} - ${tournament.name}`;
    tournamentSelect.appendChild(option);
  });

  if (payload.tournaments.length === 0) {
    setStatus("No tournaments available yet.");
  } else {
    setStatus(`Loaded ${payload.tournaments.length} tournaments.`);
  }
}

async function refreshLiveData() {
  const tournamentId = tournamentSelect.value;
  if (!tournamentId) {
    setStatus("Please select a tournament.", true);
    return;
  }

  try {
    const [matchesPayload, leaderboardPayload] = await Promise.all([
      apiRequest(`/api/public/tournaments/${tournamentId}/matches/current`),
      apiRequest(`/api/public/tournaments/${tournamentId}/leaderboard`)
    ]);

    matchesBody.innerHTML = "";
    matchesPayload.matches.forEach((match) => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${match.tableNumber}</td>
        <td>${match.player1Name}</td>
        <td>${match.player2Name ?? "BYE"}</td>
        <td>${match.result ?? "PENDING"}</td>
      `;
      matchesBody.appendChild(row);
    });

    leaderboardBody.innerHTML = "";
    leaderboardPayload.entries.forEach((entry) => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${entry.rank}</td>
        <td>${entry.playerName}</td>
        <td>${entry.score.toFixed(1)}</td>
      `;
      leaderboardBody.appendChild(row);
    });

    setStatus(`Round ${matchesPayload.roundNumber || 0} and leaderboard refreshed.`);
  } catch (error) {
    setStatus(error.message, true);
  }
}

tournamentSelect.addEventListener("change", refreshLiveData);
document.getElementById("live-refresh-btn").addEventListener("click", refreshLiveData);

(async () => {
  try {
    await loadTournaments();
    await refreshLiveData();
  } catch (error) {
    setStatus(error.message, true);
  }
})();
