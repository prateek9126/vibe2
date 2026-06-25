// Automatically point API calls to port 8080 if served from a different port (like Live Server)
(function() {
    const API_BASE = (window.location.hostname === 'localhost' || window.location.hostname.startsWith('127.')) && window.location.port !== '8080' ? window.location.protocol + '//' + window.location.hostname + ':' + '8080' : '';
    const originalFetch = window.fetch;
    window.fetch = function(url, options) {
        if (typeof url === 'string' && url.startsWith('/api/')) {
            url = API_BASE + url;
        }
        if (!options) options = {};
        options.credentials = 'include';
        return originalFetch(url, options);
    };
})();

// 1. Application State
const state = {
    user: null,
    tasks: [],
    goals: [],
    habits: [],
    notifications: [],
    activeTab: 'dashboard-tab',
    activeCategoryFilter: 'ALL',
    workloadChart: null
};

// 2. DOM Elements Selection
const elements = {
    // Auth Views
    authView: document.getElementById('auth-view'),
    appView: document.getElementById('app-view'),
    loginForm: document.getElementById('login-form'),
    signupForm: document.getElementById('signup-form'),
    switchToSignup: document.getElementById('switch-to-signup'),
    switchToLogin: document.getElementById('switch-to-login'),
    loginEmail: document.getElementById('login-email'),
    loginPassword: document.getElementById('login-password'),
    signupName: document.getElementById('signup-name'),
    signupEmail: document.getElementById('signup-email'),
    signupPassword: document.getElementById('signup-password'),
    userDisplayName: document.getElementById('user-display-name'),
    btnLogout: document.getElementById('btn-logout'),

    // Navigation & Views
    navItems: document.querySelectorAll('.nav-item'),
    tabPanes: document.querySelectorAll('.tab-pane'),
    viewTitle: document.getElementById('view-title'),

    // Voice Command
    btnVoiceCommand: document.getElementById('btn-voice-command'),
    voiceIndicator: document.getElementById('voice-indicator'),

    // Notifications
    btnNotifications: document.getElementById('btn-notifications'),
    notificationCount: document.getElementById('notification-count'),
    notificationDropdown: document.getElementById('notification-dropdown'),
    notificationList: document.getElementById('notification-list'),
    markAllRead: document.getElementById('mark-all-read'),

    // Dashboard KPIs
    kpiScore: document.getElementById('kpi-score'),
    kpiTasks: document.getElementById('kpi-tasks'),
    tasksProgressBar: document.getElementById('tasks-progress-bar'),
    kpiTasksPct: document.getElementById('kpi-tasks-pct'),
    kpiGoals: document.getElementById('kpi-goals'),
    kpiStreaks: document.getElementById('kpi-streaks'),
    kpiHabitsToday: document.getElementById('kpi-habits-today'),
    insightsContainer: document.getElementById('insights-container'),
    btnRefreshInsights: document.getElementById('btn-refresh-insights'),

    // Tasks tab
    smartTaskForm: document.getElementById('smart-task-form'),
    smartTaskInput: document.getElementById('smart-task-input'),
    filterBtns: document.querySelectorAll('.filter-btn'),
    btnAiPrioritize: document.getElementById('btn-ai-prioritize'),
    btnOpenTaskModal: document.getElementById('btn-open-task-modal'),
    listTodo: document.getElementById('list-todo'),
    listProgress: document.getElementById('list-progress'),
    listCompleted: document.getElementById('list-completed'),
    countTodo: document.getElementById('count-todo'),
    countProgress: document.getElementById('count-progress'),
    countCompleted: document.getElementById('count-completed'),

    // Calendar
    btnAiSchedule: document.getElementById('btn-ai-schedule'),
    calendarMonthName: document.getElementById('calendar-month-name'),
    calendarDaysGrid: document.getElementById('calendar-days-grid'),

    // Goals & Habits
    goalsContainer: document.getElementById('goals-container'),
    habitsContainer: document.getElementById('habits-container'),
    btnAddGoalModal: document.getElementById('btn-add-goal-modal'),
    btnAddHabitModal: document.getElementById('btn-add-habit-modal'),

    // Chatbot
    chatMessagesContainer: document.getElementById('chat-messages-container'),
    chatInputForm: document.getElementById('chat-input-form'),
    chatInputField: document.getElementById('chat-input-field'),
    suggestionBtns: document.querySelectorAll('.suggestion-btn'),

    // Modals
    taskModal: document.getElementById('task-modal'),
    taskModalForm: document.getElementById('task-modal-form'),
    taskModalTitle: document.getElementById('task-modal-title'),
    taskEditId: document.getElementById('task-edit-id'),
    taskTitleInput: document.getElementById('task-title'),
    taskDescInput: document.getElementById('task-desc'),
    taskPriorityInput: document.getElementById('task-priority'),
    taskCategoryInput: document.getElementById('task-category'),
    taskDueInput: document.getElementById('task-due'),
    taskWorkloadInput: document.getElementById('task-workload'),
    taskStatusInput: document.getElementById('task-status'),
    btnCancelTask: document.getElementById('btn-cancel-task'),
    btnCloseTaskModal: document.getElementById('btn-close-task-modal'),

    goalModal: document.getElementById('goal-modal'),
    goalForm: document.getElementById('goal-form'),
    btnCloseGoalModal: document.getElementById('btn-close-goal-modal'),
    btnCancelGoal: document.getElementById('btn-cancel-goal'),

    habitModal: document.getElementById('habit-modal'),
    habitForm: document.getElementById('habit-form'),
    btnCloseHabitModal: document.getElementById('btn-close-habit-modal'),
    btnCancelHabit: document.getElementById('btn-cancel-habit')
};

// 3. Initialization
document.addEventListener('DOMContentLoaded', () => {
    // Start directly on the dashboard in demo mode
    state.user = { fullName: "Demo User", username: "demo" };
    showAppView();
    setupEventListeners();
});

/*
// Session check
async function checkSession() {
    // Temporarily bypass authentication: default to Demo User immediately
    state.user = { fullName: "Demo User", username: "demo" };
    showAppView();

    // Asynchronously fetch current user info from backend to sync details
    try {
        const res = await fetch('/api/auth/me');
        if (res.ok) {
            state.user = await res.json();
            if (elements.userDisplayName) {
                elements.userDisplayName.textContent = state.user.fullName;
            }
        }
    } catch (e) {
        console.error("Session check offline, using local fallback", e);
    }
}

// 4. Auth Controllers
elements.switchToSignup.addEventListener('click', (e) => {
    e.preventDefault();
    elements.loginForm.classList.add('hidden');
    elements.signupForm.classList.remove('hidden');
});

elements.switchToLogin.addEventListener('click', (e) => {
    e.preventDefault();
    elements.signupForm.classList.add('hidden');
    elements.loginForm.classList.remove('hidden');
});

elements.loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = elements.loginEmail.value;
    const password = elements.loginPassword.value;

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: email, password })
        });
        if (res.ok) {
            state.user = await res.json();
            showAppView();
            elements.loginEmail.value = '';
            elements.loginPassword.value = '';
        } else {
            const data = await res.json();
            alert(data.error || 'Login failed');
        }
    } catch (e) {
        alert('An error occurred during login');
    }
});

elements.signupForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fullName = elements.signupName.value;
    const email = elements.signupEmail.value;
    const password = elements.signupPassword.value;

    try {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: email, password, fullName })
        });
        if (res.ok) {
            state.user = await res.json();
            showAppView();
            elements.signupName.value = '';
            elements.signupEmail.value = '';
            elements.signupPassword.value = '';
        } else {
            const data = await res.json();
            alert(data.error || 'Signup failed');
        }
    } catch (e) {
        alert('An error occurred during signup');
    }
});
*/

function showAuthView() {
    if (elements.appView) elements.appView.classList.add('hidden');
    if (elements.authView) elements.authView.classList.remove('hidden');
}

function showAppView() {
    if (elements.authView) elements.authView.classList.add('hidden');
    if (elements.appView) elements.appView.classList.remove('hidden');
    if (elements.userDisplayName && state.user) {
        elements.userDisplayName.textContent = state.user.fullName;
    }
    loadAppData();
}

// 5. Data Fetchers
async function loadAppData() {
    try {
        await Promise.all([
            fetchTasks().catch(e => console.error("fetchTasks failed", e)),
            fetchGoals().catch(e => console.error("fetchGoals failed", e)),
            fetchHabits().catch(e => console.error("fetchHabits failed", e)),
            fetchInsights().catch(e => console.error("fetchInsights failed", e)),
            fetchReport().catch(e => console.error("fetchReport failed", e))
        ]);
        renderAll();
    } catch (e) {
        console.error('Error loading app data', e);
    }
}

async function fetchTasks() {
    const res = await fetch('/api/tasks');
    if (res.ok) state.tasks = await res.json();
}

async function fetchGoals() {
    const res = await fetch('/api/goals');
    if (res.ok) state.goals = await res.json();
}

async function fetchHabits() {
    const res = await fetch('/api/habits');
    if (res.ok) state.habits = await res.json();
}

async function fetchInsights() {
    try {
        const res = await fetch('/api/ai/insights');
        if (res.ok) {
            const insights = await res.json();
            renderInsights(insights);
        } else {
            console.warn("fetchInsights failed: " + res.status);
            elements.insightsContainer.innerHTML = '<div class="insight-item">⚠️ Failed to load insights from backend.</div>';
        }
    } catch (e) {
        console.error("fetchInsights error", e);
        elements.insightsContainer.innerHTML = '<div class="insight-item">⚠️ Insights connection offline.</div>';
    }
}

async function fetchReport() {
    try {
        const res = await fetch('/api/ai/report');
        if (res.ok) {
            const report = await res.json();
            renderKPIs(report);
            renderWorkloadChart(report.workloadChartData);
        } else {
            console.warn("fetchReport failed: " + res.status);
        }
    } catch (e) {
        console.error("fetchReport error", e);
    }
}

// 6. Navigation Tabs
if (elements.navItems) {
    elements.navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const target = item.getAttribute('data-target');
            
            elements.navItems.forEach(i => i.classList.remove('active'));
            item.classList.add('active');

            if (elements.tabPanes) {
                elements.tabPanes.forEach(pane => {
                    pane.classList.remove('active');
                    if (pane.id === target) pane.classList.add('active');
                });
            }

            state.activeTab = target;
            if (elements.viewTitle) {
                elements.viewTitle.textContent = item.textContent.trim();
            }
            
            // Refresh target tabs
            if (target === 'calendar-tab') renderCalendar();
            if (target === 'goals-tab') {
                renderGoals();
                renderHabits();
            }
        });
    });
}

// 7. Event Listeners Setup
function setupEventListeners() {
    // Notifications trigger
    if (elements.btnNotifications) {
        elements.btnNotifications.addEventListener('click', (e) => {
            e.stopPropagation();
            if (elements.notificationDropdown) {
                elements.notificationDropdown.classList.toggle('hidden');
            }
        });
    }

    document.addEventListener('click', () => {
        if (elements.notificationDropdown) {
            elements.notificationDropdown.classList.add('hidden');
        }
    });

    if (elements.notificationDropdown) {
        elements.notificationDropdown.addEventListener('click', (e) => {
            e.stopPropagation();
        });
    }

    if (elements.markAllRead) {
        elements.markAllRead.addEventListener('click', (e) => {
            e.preventDefault();
            state.notifications = [];
            renderNotifications();
        });
    }

    // Refresh insights button
    if (elements.btnRefreshInsights) {
        elements.btnRefreshInsights.addEventListener('click', () => {
            if (elements.insightsContainer) {
                elements.insightsContainer.innerHTML = '<div class="loading-insights"><i class="fa-solid fa-spinner fa-spin"></i> Generating recommendations...</div>';
            }
            fetchInsights();
        });
    }

    // Filters for tasks
    if (elements.filterBtns) {
        elements.filterBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                elements.filterBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                state.activeCategoryFilter = btn.getAttribute('data-category');
                renderTasks();
            });
        });
    }

    // AI prioritize task button
    if (elements.btnAiPrioritize) {
        elements.btnAiPrioritize.addEventListener('click', async () => {
            elements.btnAiPrioritize.disabled = true;
            elements.btnAiPrioritize.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Analyzing...';
            const res = await fetch('/api/tasks/prioritize', { method: 'POST' });
            if (res.ok) {
                state.tasks = await res.json();
                pushLocalNotification("AI prioritization ran successfully. Tasks sorted by urgency score.", "AI_INSIGHT");
                loadAppData();
            }
            elements.btnAiPrioritize.disabled = false;
            elements.btnAiPrioritize.innerHTML = '<i class="fa-solid fa-arrow-down-9-1"></i> AI Prioritize';
        });
    }

    // NLP Smart Add Task
    if (elements.smartTaskForm) {
        elements.smartTaskForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (elements.smartTaskInput) {
                const text = elements.smartTaskInput.value;
                elements.smartTaskInput.value = '';

                try {
                    const res = await fetch('/api/tasks', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ nlpText: text })
                    });
                    if (res.ok) {
                        const newTask = await res.json();
                        pushLocalNotification(`AI added task: "${newTask.title}" via NLP parsing.`, "INFO");
                        loadAppData();
                    } else {
                        alert("Could not parse task");
                    }
                } catch (e) {
                    console.error(e);
                }
            }
        });
    }

    // AI schedule button
    if (elements.btnAiSchedule) {
        elements.btnAiSchedule.addEventListener('click', async () => {
            elements.btnAiSchedule.disabled = true;
            elements.btnAiSchedule.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Optimizing...';
            const res = await fetch('/api/tasks/schedule', { method: 'POST' });
            if (res.ok) {
                state.tasks = await res.json();
                pushLocalNotification("AI Scheduling optimized! Task dates adjusted to fit workload.", "AI_INSIGHT");
                loadAppData();
                renderCalendar();
            }
            elements.btnAiSchedule.disabled = false;
            elements.btnAiSchedule.innerHTML = '<i class="fa-solid fa-calendar-check"></i> AI Smart Schedule';
        });
    }

    // Modals visibility logic
    // Task modal
    if (elements.btnOpenTaskModal) elements.btnOpenTaskModal.addEventListener('click', () => openTaskModal());
    if (elements.btnCloseTaskModal) elements.btnCloseTaskModal.addEventListener('click', () => closeTaskModal());
    if (elements.btnCancelTask) elements.btnCancelTask.addEventListener('click', () => closeTaskModal());
    if (elements.taskModalForm) elements.taskModalForm.addEventListener('submit', handleTaskFormSubmit);

    // Goal modal
    if (elements.btnAddGoalModal) {
        elements.btnAddGoalModal.addEventListener('click', () => {
            if (elements.goalModal) elements.goalModal.classList.remove('hidden');
        });
    }
    if (elements.btnCloseGoalModal) {
        elements.btnCloseGoalModal.addEventListener('click', () => {
            if (elements.goalModal) elements.goalModal.classList.add('hidden');
        });
    }
    if (elements.btnCancelGoal) {
        elements.btnCancelGoal.addEventListener('click', () => {
            if (elements.goalModal) elements.goalModal.classList.add('hidden');
        });
    }
    if (elements.goalForm) elements.goalForm.addEventListener('submit', handleGoalFormSubmit);

    // Habit modal
    if (elements.btnAddHabitModal) {
        elements.btnAddHabitModal.addEventListener('click', () => {
            if (elements.habitModal) elements.habitModal.classList.remove('hidden');
        });
    }
    if (elements.btnCloseHabitModal) {
        elements.btnCloseHabitModal.addEventListener('click', () => {
            if (elements.habitModal) elements.habitModal.classList.add('hidden');
        });
    }
    if (elements.btnCancelHabit) {
        elements.btnCancelHabit.addEventListener('click', () => {
            if (elements.habitModal) elements.habitModal.classList.add('hidden');
        });
    }
    if (elements.habitForm) elements.habitForm.addEventListener('submit', handleHabitFormSubmit);

    // Chatbot query
    if (elements.chatInputForm) elements.chatInputForm.addEventListener('submit', handleChatbotSubmit);
    if (elements.suggestionBtns) {
        elements.suggestionBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const query = btn.getAttribute('data-query');
                sendChatbotQuery(query);
            });
        });
    }

    // Log Out Button Click
    if (elements.btnLogout) {
        elements.btnLogout.addEventListener('click', () => {
            alert("Logout functionality is disabled in demo mode.");
        });
    }

    // Voice Commands integration
    setupVoiceCommands();
}

// 8. Voice Commands Engine (Web Speech API)
function setupVoiceCommands() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        if (elements.btnVoiceCommand) {
            elements.btnVoiceCommand.style.display = 'none';
        }
        return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.lang = 'en-US';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    if (elements.btnVoiceCommand) {
        elements.btnVoiceCommand.addEventListener('click', () => {
            elements.btnVoiceCommand.classList.toggle('active');
            if (elements.btnVoiceCommand.classList.contains('active')) {
                if (elements.voiceIndicator) {
                    elements.voiceIndicator.classList.remove('hidden');
                }
                recognition.start();
            } else {
                if (elements.voiceIndicator) {
                    elements.voiceIndicator.classList.add('hidden');
                }
                recognition.stop();
            }
        });
    }

    recognition.onresult = (event) => {
        const speechResult = event.results[0][0].transcript;
        processVoiceCommand(speechResult);
    };

    recognition.onerror = (event) => {
        console.error('Speech recognition error', event.error);
        stopListening();
    };

    recognition.onend = () => {
        stopListening();
    };

    function stopListening() {
        elements.btnVoiceCommand.classList.remove('active');
        elements.voiceIndicator.classList.add('hidden');
    }
}

async function processVoiceCommand(command) {
    const cleanCommand = command.toLowerCase().trim();
    pushLocalNotification(`Voice command received: "${command}"`, "INFO");

    if (cleanCommand.startsWith("add task")) {
        const text = command.replace(/^(add task)/i, "").trim();
        elements.smartTaskInput.value = text;
        // submit programmatically
        elements.smartTaskForm.dispatchEvent(new Event('submit'));
    } else if (cleanCommand.includes("prioritize")) {
        elements.btnAiPrioritize.click();
    } else if (cleanCommand.includes("schedule")) {
        elements.btnAiSchedule.click();
    } else if (cleanCommand.includes("show dashboard")) {
        document.querySelector('[data-target="dashboard-tab"]').click();
    } else if (cleanCommand.includes("show tasks")) {
        document.querySelector('[data-target="tasks-tab"]').click();
    } else if (cleanCommand.includes("show calendar")) {
        document.querySelector('[data-target="calendar-tab"]').click();
    } else if (cleanCommand.includes("show goals")) {
        document.querySelector('[data-target="goals-tab"]').click();
    } else if (cleanCommand.includes("show chat")) {
        document.querySelector('[data-target="chatbot-tab"]').click();
    } else {
        // Send to chatbot
        sendChatbotQuery(command);
        document.querySelector('[data-target="chatbot-tab"]').click();
    }
}

// 9. Renders & Views Functions
function renderAll() {
    renderTasks();
    renderCalendar();
    renderGoals();
    renderHabits();
    renderNotifications();
}

function renderKPIs(report) {
    // Score gauge progress conic gradient
    const scoreVal = report.productivityScore || 0;
    elements.kpiScore.textContent = scoreVal + '%';
    document.querySelector('.circular-progress').style.background = `conic-gradient(var(--primary) ${scoreVal * 3.6}deg, var(--bg-secondary) 0deg)`;

    // Tasks completed stats
    const activeCount = report.activeTasksCount || 0;
    const completedCount = report.completedTasksCount || 0;
    const totalCount = activeCount + completedCount;
    elements.kpiTasks.textContent = `${completedCount} / ${totalCount}`;
    
    let pct = 0;
    if (totalCount > 0) pct = Math.round((completedCount / totalCount) * 100);
    elements.tasksProgressBar.style.width = pct + '%';
    elements.kpiTasksPct.textContent = `${pct}% completed`;

    // Goals count
    elements.kpiGoals.textContent = state.goals.length;

    // Habits consistency
    const totalHabits = report.totalHabits || 0;
    const habitsCompleted = report.completedHabitsToday || 0;
    elements.kpiHabitsToday.textContent = `${habitsCompleted} / ${totalHabits} completed today`;

    // Highest Habit Streak
    const maxStreak = state.habits.reduce((max, h) => h.streak > max ? h.streak : max, 0);
    elements.kpiStreaks.textContent = `${maxStreak} 🔥`;
}

function renderInsights(insights) {
    if (!insights || insights.length === 0) {
        elements.insightsContainer.innerHTML = '<div class="insight-item">✨ You are all caught up! Workload is well balanced.</div>';
        return;
    }
    
    elements.insightsContainer.innerHTML = insights.map(insight => `
        <div class="insight-item">
            ${insight}
        </div>
    `).join('');
}

function renderWorkloadChart(chartData) {
    if (!chartData) return;
    try {
        if (typeof Chart === 'undefined') {
            console.warn("Chart.js is not loaded. Skipping rendering workload chart.");
            return;
        }
        const ctx = document.getElementById('workloadChart').getContext('2d');
        
        const labels = Object.keys(chartData);
        const data = Object.values(chartData);

        if (state.workloadChart) {
            state.workloadChart.destroy();
        }

        state.workloadChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Workload Hours Estimate',
                    data: data,
                    backgroundColor: 'rgba(79, 70, 229, 0.65)',
                    borderColor: 'rgb(79, 70, 229)',
                    borderWidth: 1,
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        suggestedMax: 6,
                        grid: { color: '#f1f5f9' }
                      },
                    x: {
                        grid: { display: false }
                    }
                }
            }
        });
    } catch (e) {
        console.error("Error drawing workload chart", e);
    }
}

// Render task list in Kanban
function renderTasks() {
    elements.listTodo.innerHTML = '';
    elements.listProgress.innerHTML = '';
    elements.listCompleted.innerHTML = '';

    const filtered = state.tasks.filter(t => {
        if (state.activeCategoryFilter === 'ALL') return true;
        return t.category === state.activeCategoryFilter;
    });

    let counts = { TODO: 0, IN_PROGRESS: 0, COMPLETED: 0 };

    filtered.forEach(task => {
        counts[task.status]++;
        const card = createTaskCard(task);
        if (task.status === 'TODO') elements.listTodo.appendChild(card);
        else if (task.status === 'IN_PROGRESS') elements.listProgress.appendChild(card);
        else if (task.status === 'COMPLETED') elements.listCompleted.appendChild(card);
    });

    elements.countTodo.textContent = counts.TODO;
    elements.countProgress.textContent = counts.IN_PROGRESS;
    elements.countCompleted.textContent = counts.COMPLETED;
}

function createTaskCard(task) {
    const div = document.createElement('div');
    div.className = 'task-card';
    div.draggable = true;

    // Format due date nicely
    const date = new Date(task.dueDate);
    const formattedDate = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

    // Deadline predicted risk classes
    let riskClass = 'risk-on-time';
    let riskText = 'On Time';
    if (task.predictedDeadlineStatus === 'RISK_OF_DELAY') {
        riskClass = 'risk-at-risk';
        riskText = 'Risk of Delay';
    } else if (task.predictedDeadlineStatus === 'OVERDUE') {
        riskClass = 'risk-overdue';
        riskText = 'Overdue';
    }

    div.innerHTML = `
        <div class="task-card-header">
            <span class="task-category-tag">${task.category}</span>
            <div class="task-actions-menu">
                <button class="btn-card-menu" onclick="editTask(${task.id})"><i class="fa-solid fa-pen"></i></button>
                <button class="btn-card-menu" onclick="deleteTask(${task.id})" style="color: var(--danger)"><i class="fa-solid fa-trash-can"></i></button>
            </div>
        </div>
        <h4>${task.title}</h4>
        <p>${task.description || 'No description provided.'}</p>
        <div class="task-card-footer">
            <span class="deadline-risk-indicator ${riskClass}">
                <i class="fa-solid fa-circle" style="font-size: 8px"></i> ${riskText}
            </span>
            <div class="task-meta-stats">
                <span class="score-badge" title="AI Priority Score">AI: ${task.aiPriorityScore}</span>
                <span title="Estimated Workload Hours"><i class="fa-regular fa-clock"></i> ${task.workloadHours}h</span>
            </div>
        </div>
        <div style="font-size: 11px; color: var(--text-muted)">Due: ${formattedDate}</div>
    `;

    // Simple Drag and Drop listeners
    div.addEventListener('dragstart', (e) => {
        e.dataTransfer.setData('text/plain', task.id);
    });

    return div;
}

// Kanban drag-and-drop registration
['todo', 'progress', 'completed'].forEach(statusKey => {
    const col = document.getElementById(`col-${statusKey}`);
    col.addEventListener('dragover', (e) => e.preventDefault());
    col.addEventListener('drop', async (e) => {
        e.preventDefault();
        const taskId = e.dataTransfer.getData('text/plain');
        const statusMap = { 'todo': 'TODO', 'progress': 'IN_PROGRESS', 'completed': 'COMPLETED' };
        
        // Update task status in client state
        const task = state.tasks.find(t => t.id == taskId);
        if (task && task.status !== statusMap[statusKey]) {
            task.status = statusMap[statusKey];
            renderTasks();
            
            // Save updates in DB
            await fetch(`/api/tasks/${taskId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ status: statusMap[statusKey] })
            });
            loadAppData();
        }
    });
});

// Render Calendar tab
function renderCalendar() {
    elements.calendarDaysGrid.innerHTML = '';
    
    // Hardcode calendar view to Current Month (June 2026 as per time metadata)
    const year = 2026;
    const month = 5; // 0-indexed: 5 = June
    elements.calendarMonthName.textContent = "June 2026 Calendar Planner";

    // Find start day of month (Monday-Sunday)
    const firstDay = new Date(year, month, 1);
    let startOffset = firstDay.getDay() - 1; // Align to Mon-Sun
    if (startOffset < 0) startOffset = 6; // Sunday index

    // Days in current month
    const totalDays = new Date(year, month + 1, 0).getDate();

    // Render preceding empty days (or other month cells)
    for (let i = 0; i < startOffset; i++) {
        const emptyCell = document.createElement('div');
        emptyCell.className = 'calendar-day-cell other-month';
        elements.calendarDaysGrid.appendChild(emptyCell);
    }

    // Render actual month days
    for (let day = 1; day <= totalDays; day++) {
        const cell = document.createElement('div');
        cell.className = 'calendar-day-cell';
        
        const dayNum = document.createElement('div');
        dayNum.className = 'day-number';
        dayNum.textContent = day;
        cell.appendChild(dayNum);

        // Filter tasks due on this day (comparing day, month, year)
        state.tasks.forEach(task => {
            if (task.dueDate) {
                const taskDate = new Date(task.dueDate);
                if (taskDate.getDate() === day && taskDate.getMonth() === month && taskDate.getFullYear() === year) {
                    const pill = document.createElement('div');
                    pill.className = 'calendar-task-tag';
                    pill.title = task.title;
                    pill.textContent = task.title;
                    cell.appendChild(pill);
                }
            }
        });

        elements.calendarDaysGrid.appendChild(cell);
    }
}

// Render Goals tab
function renderGoals() {
    elements.goalsContainer.innerHTML = '';
    if (state.goals.length === 0) {
        elements.goalsContainer.innerHTML = '<p class="empty-noti">No active goals. Click "New Goal" to get started.</p>';
        return;
    }

    state.goals.forEach(goal => {
        const div = document.createElement('div');
        div.className = 'goal-item';

        const pct = goal.targetValue > 0 ? Math.round((goal.currentValue / goal.targetValue) * 100) : 0;
        
        div.innerHTML = `
            <div class="goal-info">
                <div>
                    <h4>${goal.title}</h4>
                    <p style="font-size: 12px; margin-top: 4px">${goal.description || 'No description'}</p>
                </div>
                <div style="display: flex; gap: 8px">
                    <button class="btn btn-outline btn-small" onclick="incrementGoal(${goal.id})"><i class="fa-solid fa-plus"></i></button>
                    <button class="btn btn-outline btn-small" onclick="deleteGoal(${goal.id})" style="color: var(--danger)"><i class="fa-solid fa-trash"></i></button>
                </div>
            </div>
            <div class="progress-bar-container" style="margin-top: 8px">
                <div class="progress-bar-fill" style="width: ${Math.min(100, pct)}%"></div>
            </div>
            <div class="goal-progress-details">
                <span>Progress: ${goal.currentValue} / ${goal.targetValue} ${goal.unit}</span>
                <span>${pct}% Complete</span>
            </div>
        `;
        elements.goalsContainer.appendChild(div);
    });
}

// Render Habits tab
function renderHabits() {
    elements.habitsContainer.innerHTML = '';
    if (state.habits.length === 0) {
        elements.habitsContainer.innerHTML = '<p class="empty-noti">No active habits. Click "New Habit" to track your daily routine.</p>';
        return;
    }

    state.habits.forEach(habit => {
        const div = document.createElement('div');
        
        const isCompletedToday = habit.lastCompletedDate && (habit.lastCompletedDate === new Date().toISOString().split('T')[0]);
        div.className = `habit-item ${isCompletedToday ? 'completed' : ''}`;

        div.innerHTML = `
            <div class="habit-details">
                <button class="habit-check-btn ${isCompletedToday ? 'checked' : ''}" onclick="completeHabit(${habit.id})">
                    ${isCompletedToday ? '<i class="fa-solid fa-check"></i>' : ''}
                </button>
                <div>
                    <h4 style="text-decoration: ${isCompletedToday ? 'line-through' : 'none'}">${habit.name}</h4>
                    <span style="font-size: 10px; color: var(--text-muted)">${habit.frequency}</span>
                </div>
            </div>
            <div class="habit-meta">
                <span class="habit-streak-display">${habit.streak} 🔥</span>
                <button class="btn-card-menu" onclick="deleteHabit(${habit.id})" style="color: var(--danger)"><i class="fa-solid fa-trash-can"></i></button>
            </div>
        `;
        elements.habitsContainer.appendChild(div);
    });
}

// Render notifications
function renderNotifications() {
    if (state.notifications.length === 0) {
        elements.notificationCount.classList.add('hidden');
        elements.notificationList.innerHTML = '<p class="empty-noti">No new notifications</p>';
        return;
    }

    elements.notificationCount.classList.remove('hidden');
    elements.notificationCount.textContent = state.notifications.length;

    elements.notificationList.innerHTML = state.notifications.map(noti => `
        <div class="noti-item unread">
            <p>${noti.message}</p>
            <span style="font-size: 9px; color: var(--text-muted); display: block; margin-top: 4px">Just Now</span>
        </div>
    `).join('');
}

function pushLocalNotification(message, type) {
    state.notifications.unshift({ message, type });
    renderNotifications();
}

// 10. Modals Logic & CRUD Submissions
function openTaskModal(taskId = null) {
    if (taskId) {
        const task = state.tasks.find(t => t.id == taskId);
        if (task) {
            elements.taskModalTitle.textContent = "Edit Task";
            elements.taskEditId.value = task.id;
            elements.taskTitleInput.value = task.title;
            elements.taskDescInput.value = task.description || '';
            elements.taskPriorityInput.value = task.priority;
            elements.taskCategoryInput.value = task.category;
            
            // Format LocalDateTime back to string for datetime-local
            if (task.dueDate) {
                elements.taskDueInput.value = task.dueDate.substring(0, 16);
            }
            
            elements.taskWorkloadInput.value = task.workloadHours;
            elements.taskStatusInput.value = task.status;
            elements.taskStatusInput.parentElement.classList.remove('hidden');
        }
    } else {
        elements.taskModalTitle.textContent = "New Task";
        elements.taskEditId.value = '';
        elements.taskModalForm.reset();
        
        // Pre-fill next hour as default due date
        const defaultDue = new Date(Date.now() + 24 * 60 * 60 * 1000);
        defaultDue.setHours(17, 0, 0, 0); // tomorrow at 5pm
        
        // Format ISO String
        const offset = defaultDue.getTimezoneOffset();
        const localDue = new Date(defaultDue.getTime() - (offset * 60 * 1000));
        elements.taskDueInput.value = localDue.toISOString().substring(0, 16);
        
        elements.taskStatusInput.value = 'TODO';
        elements.taskStatusInput.parentElement.classList.add('hidden');
    }
    elements.taskModal.classList.remove('hidden');
}

function closeTaskModal() {
    elements.taskModal.classList.add('hidden');
}

async function handleTaskFormSubmit(e) {
    e.preventDefault();
    const taskId = elements.taskEditId.value;

    const taskData = {
        title: elements.taskTitleInput.value,
        description: elements.taskDescInput.value,
        priority: elements.taskPriorityInput.value,
        category: elements.taskCategoryInput.value,
        dueDate: elements.taskDueInput.value,
        workloadHours: parseFloat(elements.taskWorkloadInput.value),
        status: elements.taskStatusInput.value
    };

    let url = '/api/tasks';
    let method = 'POST';

    if (taskId) {
        url = `/api/tasks/${taskId}`;
        method = 'PUT';
    }

    const res = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(taskData)
    });

    if (res.ok) {
        closeTaskModal();
        loadAppData();
    }
}

// Delete Task
window.deleteTask = async (id) => {
    if (confirm("Are you sure you want to delete this task?")) {
        const res = await fetch(`/api/tasks/${id}`, { method: 'DELETE' });
        if (res.ok) {
            loadAppData();
        }
    }
};

// Edit Task Trigger
window.editTask = (id) => {
    openTaskModal(id);
};

// Increment Goal Progress
window.incrementGoal = async (id) => {
    const goal = state.goals.find(g => g.id == id);
    if (goal) {
        const newVal = goal.currentValue + 1;
        const res = await fetch(`/api/goals/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ currentValue: newVal })
        });
        if (res.ok) {
            loadAppData();
        }
    }
};

// Delete Goal
window.deleteGoal = async (id) => {
    if (confirm("Delete this goal?")) {
        const res = await fetch(`/api/goals/${id}`, { method: 'DELETE' });
        if (res.ok) {
            loadAppData();
        }
    }
};

// Handle Goal Creation
async function handleGoalFormSubmit(e) {
    e.preventDefault();
    const goalData = {
        title: document.getElementById('goal-title').value,
        description: document.getElementById('goal-desc').value,
        targetValue: parseInt(document.getElementById('goal-target').value),
        unit: document.getElementById('goal-unit').value,
        targetDate: document.getElementById('goal-due').value
    };

    const res = await fetch('/api/goals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(goalData)
    });

    if (res.ok) {
        elements.goalModal.classList.add('hidden');
        elements.goalForm.reset();
        loadAppData();
    }
}

// Complete Habit Check
window.completeHabit = async (id) => {
    const res = await fetch(`/api/habits/${id}/complete`, { method: 'POST' });
    if (res.ok) {
        loadAppData();
    }
};

// Delete Habit
window.deleteHabit = async (id) => {
    if (confirm("Delete this habit?")) {
        const res = await fetch(`/api/habits/${id}`, { method: 'DELETE' });
        if (res.ok) {
            loadAppData();
        }
    }
};

// Handle Habit Creation
async function handleHabitFormSubmit(e) {
    e.preventDefault();
    const habitData = {
        name: document.getElementById('habit-name').value,
        frequency: document.getElementById('habit-freq').value
    };

    const res = await fetch('/api/habits', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(habitData)
    });

    if (res.ok) {
        elements.habitModal.classList.add('hidden');
        elements.habitForm.reset();
        loadAppData();
    }
}

// 11. Chatbot Controllers
function handleChatbotSubmit(e) {
    e.preventDefault();
    const query = elements.chatInputField.value;
    elements.chatInputField.value = '';
    sendChatbotQuery(query);
}

async function sendChatbotQuery(query) {
    // Append user bubble
    appendChatBubble(query, 'user');
    
    // Append loading bubble
    const loadingId = appendChatBubble('<i class="fa-solid fa-spinner fa-spin"></i> Typing...', 'ai');

    try {
        const res = await fetch('/api/ai/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query })
        });

        document.getElementById(loadingId).remove();

        if (res.ok) {
            const data = await res.json();
            appendChatBubble(data.reply, 'ai');

            // Handle actions triggered from chat commands
            if (data.action === 'REFRESH_TASKS') {
                loadAppData();
            } else if (data.action === 'REFRESH_CALENDAR') {
                loadAppData();
            } else if (data.action === 'SHOW_INSIGHTS') {
                loadAppData();
            } else if (data.action === 'REFRESH_SCORE') {
                loadAppData();
            }
        } else {
            appendChatBubble("I'm sorry, I encountered an issue processing that query.", 'ai');
        }
    } catch (e) {
        document.getElementById(loadingId).remove();
        appendChatBubble("Connection issue. Please check your server connection.", 'ai');
    }
}

function appendChatBubble(text, sender) {
    const bubbleId = 'bubble-' + Date.now();
    const div = document.createElement('div');
    div.className = `chat-bubble ${sender}`;
    div.id = bubbleId;
    
    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    div.innerHTML = `
        <p>${text.replace(/\n/g, '<br>')}</p>
        <span class="chat-time">${timeStr}</span>
    `;

    elements.chatMessagesContainer.appendChild(div);
    elements.chatMessagesContainer.scrollTop = elements.chatMessagesContainer.scrollHeight;
    
    return bubbleId;
}
