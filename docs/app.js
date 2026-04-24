const leaderboardInput = document.getElementById('leaderboardInput');
const fetchButton = document.getElementById('fetchButton');
const renderButton = document.getElementById('renderButton');
const sampleButton = document.getElementById('sampleButton');
const outputSection = document.getElementById('output');
const leaderboardBody = document.getElementById('leaderboardBody');
const totalScoreLabel = document.getElementById('totalScoreLabel');

const sampleData = [
    { participant: 'Alice', totalScore: 100 },
    { participant: 'Bob', totalScore: 120 },
    { participant: 'Charlie', totalScore: 90 }
];

function renderLeaderboard(data) {
    const sorted = [...data].sort((a, b) => b.totalScore - a.totalScore || a.participant.localeCompare(b.participant));
    leaderboardBody.innerHTML = '';

    let totalScore = 0;
    sorted.forEach((row, index) => {
        totalScore += Number(row.totalScore || 0);
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${index + 1}</td><td>${row.participant}</td><td>${row.totalScore}</td>`;
        leaderboardBody.appendChild(tr);
    });

    totalScoreLabel.textContent = `Total score across all participants: ${totalScore}`;
    outputSection.classList.remove('hidden');
}

function handleRender() {
    const value = leaderboardInput.value.trim();
    if (!value) {
        alert('Please paste leaderboard JSON into the textarea.');
        return;
    }

    try {
        const data = JSON.parse(value);
        if (!Array.isArray(data)) {
            throw new Error('Leaderboard JSON must be an array.');
        }
        renderLeaderboard(data);
    } catch (error) {
        alert(`Invalid JSON: ${error.message}`);
    }
}

function handleFetch() {
    axios.get('/api/leaderboard')
        .then(response => {
            renderLeaderboard(response.data);
            leaderboardInput.value = JSON.stringify(response.data, null, 2);
        })
        .catch(err => {
            const message = err.response?.data || err.message || 'Unable to fetch leaderboard from the server.';
            alert(`Fetch failed: ${message}`);
        });
}

fetchButton.addEventListener('click', handleFetch);
renderButton.addEventListener('click', handleRender);
sampleButton.addEventListener('click', () => {
    leaderboardInput.value = JSON.stringify(sampleData, null, 2);
    renderLeaderboard(sampleData);
});
