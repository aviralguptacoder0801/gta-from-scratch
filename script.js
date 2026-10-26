/**
 * PennyWise — Student Budget Tracker
 * Core JavaScript Logic (High Performance & Zero-Lag)
 */

(function () {
  'use strict';

  // Storage key
  const STORAGE_KEY = 'pennywise_student_budget_data_v2';

  // Category Icon Mappings
  const CATEGORY_ICONS = {
    'Food & Dining': '🍔',
    'Books & Study': '📚',
    'Transport': '🚌',
    'Rent & Utilities': '🏠',
    'Entertainment': '🎬',
    'Tech & Recharge': '📱',
    'Personal Care': '🧴',
    'Miscellaneous': '🏷️'
  };

  // State
  let state = {
    monthlyBudget: 0,
    warningLimit: 0,      // Custom manually decided expenditure warning limit
    expenses: [],
    filterCategory: 'ALL',
    searchTerm: '',
    sortBy: 'newest'
  };

  // DOM Elements
  const budgetForm = document.getElementById('budgetForm');
  const monthlyBudgetInput = document.getElementById('monthlyBudgetInput');
  const budgetError = document.getElementById('budgetError');
  const budgetDisplay = document.getElementById('budgetDisplay');
  const editBudgetBtn = document.getElementById('editBudgetBtn');
  const budgetRemainingRatio = document.getElementById('budgetRemainingRatio');

  const warningLimitForm = document.getElementById('warningLimitForm');
  const warningLimitInput = document.getElementById('warningLimitInput');
  const warningLimitError = document.getElementById('warningLimitError');
  const warningLimitDisplay = document.getElementById('warningLimitDisplay');
  const limitStatusPill = document.getElementById('limitStatusPill');
  const limitCard = document.getElementById('limitCard');
  const editLimitBtn = document.getElementById('editLimitBtn');
  const limitMarkerPin = document.getElementById('limitMarkerPin');
  const limitInfoNote = document.getElementById('limitInfoNote');

  const customWarningBanner = document.getElementById('customWarningBanner');
  const customAlertTitle = document.getElementById('customAlertTitle');
  const customAlertMessage = document.getElementById('customAlertMessage');

  const expenseForm = document.getElementById('expenseForm');
  const expenseNameInput = document.getElementById('expenseNameInput');
  const expenseCategorySelect = document.getElementById('expenseCategorySelect');
  const expenseAmountInput = document.getElementById('expenseAmountInput');
  const expenseNameError = document.getElementById('expenseNameError');
  const expenseCategoryError = document.getElementById('expenseCategoryError');
  const expenseAmountError = document.getElementById('expenseAmountError');

  const totalExpensesDisplay = document.getElementById('totalExpensesDisplay');
  const expenseCountText = document.getElementById('expenseCountText');
  const dailyAvgText = document.getElementById('dailyAvgText');
  const remainingBalanceDisplay = document.getElementById('remainingBalanceDisplay');
  const balanceStatusPill = document.getElementById('balanceStatusPill');
  const balanceIcon = document.getElementById('balanceIcon');

  const meterSubtitle = document.getElementById('meterSubtitle');
  const budgetProgressBar = document.getElementById('budgetProgressBar');
  const categoryBreakdownSection = document.getElementById('categoryBreakdownSection');
  const categoryBreakdownRow = document.getElementById('categoryBreakdownRow');

  const pennyScoreValue = document.getElementById('pennyScoreValue');
  const pennyScoreBadge = document.getElementById('pennyScoreBadge');

  const budgetAlertBanner = document.getElementById('budgetAlertBanner');
  const alertIcon = document.getElementById('alertIcon');
  const alertTitle = document.getElementById('alertTitle');
  const alertMessage = document.getElementById('alertMessage');

  const expenseList = document.getElementById('expenseList');
  const emptyState = document.getElementById('emptyState');
  const filterCategorySelect = document.getElementById('filterCategorySelect');
  const sortExpenseSelect = document.getElementById('sortExpenseSelect');
  const searchExpenseInput = document.getElementById('searchExpenseInput');
  const clearSearchBtn = document.getElementById('clearSearchBtn');

  const presetChips = document.querySelectorAll('.preset-chip');
  const limitPresetChips = document.querySelectorAll('.limit-preset-chip');
  const quickTagChips = document.querySelectorAll('.quick-tag-chip');
  const quickSampleBtn = document.getElementById('quickSampleBtn');
  const emptyStateSampleBtn = document.getElementById('emptyStateSampleBtn');
  const resetAllBtn = document.getElementById('resetAllBtn');
  const toastNotification = document.getElementById('toastNotification');
  const currentYearSpan = document.getElementById('currentYear');

  let toastTimeout = null;

  // Initialize
  function init() {
    if (currentYearSpan) {
      currentYearSpan.textContent = new Date().getFullYear();
    }

    loadState();
    setupEventListeners();
    updateUI();
  }

  // Load from LocalStorage
  function loadState() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY) || localStorage.getItem('pennywise_student_budget_data_v1');
      if (saved) {
        const parsed = JSON.parse(saved);
        state.monthlyBudget = typeof parsed.monthlyBudget === 'number' ? parsed.monthlyBudget : 0;
        state.warningLimit = typeof parsed.warningLimit === 'number' ? parsed.warningLimit : (state.monthlyBudget ? Math.round(state.monthlyBudget * 0.8) : 0);
        state.expenses = Array.isArray(parsed.expenses) ? parsed.expenses : [];
      } else {
        // First launch starter setup
        state.monthlyBudget = 10000;
        state.warningLimit = 7500; // Custom 75% default limit
        state.expenses = [
          {
            id: 'demo-1',
            name: 'College Cafeteria Lunch',
            category: 'Food & Dining',
            amount: 140,
            date: new Date().toLocaleDateString('en-IN', { month: 'short', day: 'numeric' })
          },
          {
            id: 'demo-2',
            name: 'Semester Reference Books',
            category: 'Books & Study',
            amount: 850,
            date: new Date().toLocaleDateString('en-IN', { month: 'short', day: 'numeric' })
          },
          {
            id: 'demo-3',
            name: 'Metro Smart Card Pass',
            category: 'Transport',
            amount: 500,
            date: new Date().toLocaleDateString('en-IN', { month: 'short', day: 'numeric' })
          }
        ];
        saveState();
      }
    } catch (e) {
      state.monthlyBudget = 10000;
      state.warningLimit = 8000;
      state.expenses = [];
    }
  }

  // Save to LocalStorage
  function saveState() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        monthlyBudget: state.monthlyBudget,
        warningLimit: state.warningLimit,
        expenses: state.expenses
      }));
    } catch (e) {
      // Storage unavailable
    }
  }

  // Currency Formatter in Indian Rupees (₹)
  function formatINR(amount) {
    const num = Number(amount) || 0;
    return '₹' + num.toLocaleString('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  // Show Toast
  function showToast(message, type = 'success') {
    if (!toastNotification) return;
    clearTimeout(toastTimeout);
    toastNotification.textContent = message;
    toastNotification.className = `toast show ${type === 'danger' ? 'toast-danger' : 'toast-success'}`;
    toastTimeout = setTimeout(() => {
      toastNotification.className = 'toast';
    }, 2400);
  }

  // Helper to clear input error
  function clearError(inputElem, errorElem) {
    if (inputElem) inputElem.classList.remove('input-has-error');
    if (errorElem) {
      errorElem.textContent = '';
      errorElem.classList.remove('visible');
    }
  }

  // Helper to show input error
  function showError(inputElem, errorElem, message) {
    if (inputElem) {
      inputElem.classList.add('input-has-error');
      inputElem.focus();
    }
    if (errorElem) {
      errorElem.textContent = message;
      errorElem.classList.add('visible');
    }
  }

  // Setup Event Listeners
  function setupEventListeners() {
    // 1. Budget Form
    budgetForm.addEventListener('submit', (e) => {
      e.preventDefault();
      handleBudgetSubmit();
    });

    monthlyBudgetInput.addEventListener('input', () => {
      clearError(monthlyBudgetInput, budgetError);
    });

    presetChips.forEach(chip => {
      chip.addEventListener('click', () => {
        const val = Number(chip.getAttribute('data-preset'));
        if (val > 0) {
          monthlyBudgetInput.value = val;
          clearError(monthlyBudgetInput, budgetError);
          setBudget(val);
        }
      });
    });

    if (editBudgetBtn) {
      editBudgetBtn.addEventListener('click', () => {
        monthlyBudgetInput.focus();
        monthlyBudgetInput.select();
        const section = document.getElementById('budgetSettingSection');
        if (section) section.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      });
    }

    // 2. Custom Warning Limit Form
    warningLimitForm.addEventListener('submit', (e) => {
      e.preventDefault();
      handleWarningLimitSubmit();
    });

    warningLimitInput.addEventListener('input', () => {
      clearError(warningLimitInput, warningLimitError);
    });

    limitPresetChips.forEach(chip => {
      chip.addEventListener('click', () => {
        const pct = Number(chip.getAttribute('data-pct'));
        if (state.monthlyBudget > 0 && pct > 0) {
          const calculatedLimit = Math.round((state.monthlyBudget * pct) / 100);
          warningLimitInput.value = calculatedLimit;
          clearError(warningLimitInput, warningLimitError);
          setWarningLimit(calculatedLimit);
        } else if (state.monthlyBudget <= 0) {
          showToast('Please set your monthly budget first!', 'danger');
        }
      });
    });

    if (editLimitBtn) {
      editLimitBtn.addEventListener('click', () => {
        warningLimitInput.focus();
        warningLimitInput.select();
        const section = document.getElementById('warningLimitSection');
        if (section) section.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      });
    }

    // 3. Fast Quick Tag Chips
    quickTagChips.forEach(btn => {
      btn.addEventListener('click', () => {
        const name = btn.getAttribute('data-name');
        const cat = btn.getAttribute('data-cat');
        const amt = parseFloat(btn.getAttribute('data-amount'));

        if (name && cat && amt > 0) {
          addExpenseItem(name, cat, amt);
        }
      });
    });

    // 4. Expense Form
    expenseForm.addEventListener('submit', (e) => {
      e.preventDefault();
      handleAddExpenseSubmit();
    });

    expenseNameInput.addEventListener('input', () => {
      clearError(expenseNameInput, expenseNameError);
    });
    expenseCategorySelect.addEventListener('change', () => {
      clearError(expenseCategorySelect, expenseCategoryError);
    });
    expenseAmountInput.addEventListener('input', () => {
      clearError(expenseAmountInput, expenseAmountError);
    });

    // 5. Search, Filter & Sort
    if (searchExpenseInput) {
      searchExpenseInput.addEventListener('input', (e) => {
        state.searchTerm = e.target.value.trim().toLowerCase();
        if (clearSearchBtn) {
          clearSearchBtn.style.display = state.searchTerm ? 'block' : 'none';
        }
        renderExpenses();
      });
    }

    if (clearSearchBtn) {
      clearSearchBtn.addEventListener('click', () => {
        searchExpenseInput.value = '';
        state.searchTerm = '';
        clearSearchBtn.style.display = 'none';
        renderExpenses();
      });
    }

    if (filterCategorySelect) {
      filterCategorySelect.addEventListener('change', (e) => {
        state.filterCategory = e.target.value;
        renderExpenses();
      });
    }

    if (sortExpenseSelect) {
      sortExpenseSelect.addEventListener('change', (e) => {
        state.sortBy = e.target.value;
        renderExpenses();
      });
    }

    // 6. Samples & Reset
    if (quickSampleBtn) quickSampleBtn.addEventListener('click', loadSampleData);
    if (emptyStateSampleBtn) emptyStateSampleBtn.addEventListener('click', loadSampleData);
    if (resetAllBtn) resetAllBtn.addEventListener('click', handleResetAll);
  }

  // Handle Budget Form Submit
  function handleBudgetSubmit() {
    const rawValue = monthlyBudgetInput.value.trim();
    const value = parseFloat(rawValue);

    if (rawValue === '' || isNaN(value)) {
      showError(monthlyBudgetInput, budgetError, 'Please enter a valid monthly budget.');
      return;
    }
    if (value <= 0) {
      showError(monthlyBudgetInput, budgetError, 'Monthly budget must be greater than ₹0.');
      return;
    }

    setBudget(value);
  }

  function setBudget(value) {
    state.monthlyBudget = Math.round(value * 100) / 100;
    // If warning limit was not set or is 0, auto-propose 80% default
    if (state.warningLimit <= 0) {
      state.warningLimit = Math.round(state.monthlyBudget * 0.8);
      warningLimitInput.value = state.warningLimit;
    }
    saveState();
    updateUI();
    showToast(`Monthly budget set to ${formatINR(state.monthlyBudget)}!`);
  }

  // Handle Warning Limit Form Submit
  function handleWarningLimitSubmit() {
    const rawValue = warningLimitInput.value.trim();
    const value = parseFloat(rawValue);

    if (rawValue === '' || isNaN(value)) {
      showError(warningLimitInput, warningLimitError, 'Please enter a valid expenditure limit.');
      return;
    }
    if (value <= 0) {
      showError(warningLimitInput, warningLimitError, 'Warning limit must be greater than ₹0.');
      return;
    }

    setWarningLimit(value);
  }

  function setWarningLimit(value) {
    state.warningLimit = Math.round(value * 100) / 100;
    saveState();
    updateUI();
    showToast(`Expenditure warning limit set to ${formatINR(state.warningLimit)}!`);
  }

  // Handle Add Expense Form Submit
  function handleAddExpenseSubmit() {
    const name = expenseNameInput.value.trim();
    const category = expenseCategorySelect.value;
    const rawAmount = expenseAmountInput.value.trim();
    const amount = parseFloat(rawAmount);

    let hasError = false;

    if (!name) {
      showError(expenseNameInput, expenseNameError, 'Please enter an expense title.');
      hasError = true;
    } else {
      clearError(expenseNameInput, expenseNameError);
    }

    if (!category) {
      showError(expenseCategorySelect, expenseCategoryError, 'Please select a category.');
      hasError = true;
    } else {
      clearError(expenseCategorySelect, expenseCategoryError);
    }

    if (rawAmount === '' || isNaN(amount)) {
      showError(expenseAmountInput, expenseAmountError, 'Please enter a valid amount.');
      hasError = true;
    } else if (amount <= 0) {
      showError(expenseAmountInput, expenseAmountError, 'Expense amount must be greater than ₹0.');
      hasError = true;
    } else {
      clearError(expenseAmountInput, expenseAmountError);
    }

    if (hasError) return;

    addExpenseItem(name, category, amount);

    // Reset inputs
    expenseNameInput.value = '';
    expenseCategorySelect.selectedIndex = 0;
    expenseAmountInput.value = '';
    expenseNameInput.focus();
  }

  // Add Item Helper
  function addExpenseItem(name, category, amount) {
    const newExpense = {
      id: 'exp-' + Date.now() + '-' + Math.floor(Math.random() * 1000),
      name: name,
      category: category,
      amount: Math.round(amount * 100) / 100,
      timestamp: Date.now(),
      date: new Date().toLocaleDateString('en-IN', { month: 'short', day: 'numeric' })
    };

    state.expenses.unshift(newExpense);
    saveState();
    updateUI();
    showToast(`Added "${newExpense.name}" (${formatINR(newExpense.amount)})`);
  }

  // Delete Expense
  function deleteExpense(id) {
    const itemIndex = state.expenses.findIndex(e => e.id === id);
    if (itemIndex > -1) {
      const removed = state.expenses.splice(itemIndex, 1)[0];
      saveState();
      updateUI();
      showToast(`Removed "${removed.name}"`, 'danger');
    }
  }

  // Load Sample Data
  function loadSampleData() {
    state.monthlyBudget = 15000;
    state.warningLimit = 11000; // Custom manual warning limit
    state.expenses = [
      {
        id: 'sample-1',
        name: 'Semester Textbooks & Notes',
        category: 'Books & Study',
        amount: 1850,
        timestamp: Date.now() - 3600000 * 24 * 4,
        date: 'Oct 24'
      },
      {
        id: 'sample-2',
        name: 'Hostel Mess & Evening Snacks',
        category: 'Food & Dining',
        amount: 3200,
        timestamp: Date.now() - 3600000 * 24 * 3,
        date: 'Oct 23'
      },
      {
        id: 'sample-3',
        name: 'Metro Card Monthly Pass',
        category: 'Transport',
        amount: 800,
        timestamp: Date.now() - 3600000 * 24 * 2,
        date: 'Oct 21'
      },
      {
        id: 'sample-4',
        name: 'High-speed Hostel WiFi',
        category: 'Tech & Recharge',
        amount: 499,
        timestamp: Date.now() - 3600000 * 24 * 1,
        date: 'Oct 19'
      },
      {
        id: 'sample-5',
        name: 'Weekend Movie & Treat',
        category: 'Entertainment',
        amount: 350,
        timestamp: Date.now(),
        date: 'Oct 18'
      }
    ];

    monthlyBudgetInput.value = state.monthlyBudget;
    warningLimitInput.value = state.warningLimit;
    saveState();
    updateUI();
    showToast('Loaded sample budget & student expenses!');
  }

  // Reset All
  function handleResetAll() {
    const confirmed = window.confirm('Are you sure you want to reset all budget, warning limit, and expense data?');
    if (!confirmed) return;

    state.monthlyBudget = 0;
    state.warningLimit = 0;
    state.expenses = [];
    monthlyBudgetInput.value = '';
    warningLimitInput.value = '';
    expenseNameInput.value = '';
    expenseCategorySelect.selectedIndex = 0;
    expenseAmountInput.value = '';

    saveState();
    updateUI();
    showToast('All records reset.', 'danger');
  }

  // Main Reactive Engine
  function updateUI() {
    // 1. Math & Aggregations
    const totalExpenses = Math.round(
      state.expenses.reduce((sum, item) => sum + (Number(item.amount) || 0), 0) * 100
    ) / 100;
    const budget = state.monthlyBudget;
    const remainingBalance = Math.round((budget - totalExpenses) * 100) / 100;
    const warningLimit = state.warningLimit;
    const count = state.expenses.length;

    // Daily Average (based on current day of month)
    const dayOfMonth = new Date().getDate();
    const dailyAvg = dayOfMonth > 0 ? Math.round(totalExpenses / dayOfMonth) : 0;
    dailyAvgText.textContent = `~${formatINR(dailyAvg)}/day`;

    // 2. Budget Card
    budgetDisplay.textContent = formatINR(budget);
    if (!monthlyBudgetInput.value && budget > 0) {
      monthlyBudgetInput.value = budget;
    }

    // 3. Expenses Card
    totalExpensesDisplay.textContent = formatINR(totalExpenses);
    expenseCountText.textContent = `${count} ${count === 1 ? 'record' : 'records'}`;

    // 4. Remaining Balance Card
    remainingBalanceDisplay.textContent = formatINR(remainingBalance);

    // 5. Warning Limit Card
    warningLimitDisplay.textContent = warningLimit > 0 ? formatINR(warningLimit) : 'Not set';
    if (!warningLimitInput.value && warningLimit > 0) {
      warningLimitInput.value = warningLimit;
    }

    // Check if Custom Warning Limit is Breached!
    const isLimitBreached = warningLimit > 0 && totalExpenses > warningLimit;
    if (isLimitBreached) {
      limitCard.classList.add('limit-breached');
      limitStatusPill.className = 'status-pill status-danger';
      limitStatusPill.textContent = 'Breached!';
    } else if (warningLimit > 0 && totalExpenses > 0 && totalExpenses >= warningLimit * 0.85) {
      limitCard.classList.remove('limit-breached');
      limitStatusPill.className = 'status-pill status-warning';
      limitStatusPill.textContent = 'Near Limit';
    } else {
      limitCard.classList.remove('limit-breached');
      limitStatusPill.className = 'status-pill status-neutral';
      limitStatusPill.textContent = warningLimit > 0 ? 'Safe' : 'Not set';
    }

    // 6. Percentage Spent
    let percentSpent = 0;
    if (budget > 0) {
      percentSpent = Math.round((totalExpenses / budget) * 100);
    }
    budgetRemainingRatio.textContent = `${percentSpent}% spent`;
    meterSubtitle.textContent = budget > 0 
      ? `${percentSpent}% of monthly budget utilized (${formatINR(totalExpenses)} / ${formatINR(budget)})`
      : 'Set a monthly budget to calculate spending utilization';

    // Progress Bar Width & Color
    const barWidth = Math.min(Math.max(percentSpent, 0), 100);
    budgetProgressBar.style.width = `${barWidth}%`;
    budgetProgressBar.className = 'progress-bar';

    // Position Custom Warning Limit Marker Pin on the track
    if (budget > 0 && warningLimit > 0) {
      const pinPct = Math.min(Math.round((warningLimit / budget) * 100), 100);
      limitMarkerPin.style.display = 'block';
      limitMarkerPin.style.left = `${pinPct}%`;
      limitMarkerPin.title = `Alert Limit: ${formatINR(warningLimit)} (${pinPct}%)`;
      if (limitInfoNote) {
        limitInfoNote.textContent = `📌 Warning trigger set at ${formatINR(warningLimit)} (${pinPct}% of your ₹${budget.toLocaleString('en-IN')} budget).`;
      }
    } else {
      limitMarkerPin.style.display = 'none';
    }

    // 7. Balance Card Status Pill
    if (remainingBalance < 0) {
      balanceStatusPill.className = 'status-pill status-danger';
      balanceStatusPill.textContent = 'Over Budget';
      balanceIcon.textContent = '🚨';
      remainingBalanceDisplay.style.color = '#f87171';
      budgetProgressBar.classList.add('progress-danger');
    } else if (budget > 0 && percentSpent >= 80) {
      balanceStatusPill.className = 'status-pill status-warning';
      balanceStatusPill.textContent = 'Low Balance';
      balanceIcon.textContent = '⚠️';
      remainingBalanceDisplay.style.color = '#fbbf24';
      budgetProgressBar.classList.add('progress-warning');
    } else {
      balanceStatusPill.className = 'status-pill status-healthy';
      balanceStatusPill.textContent = 'In Budget';
      balanceIcon.textContent = '⚖️';
      remainingBalanceDisplay.style.color = '#34d399';
    }

    // 8. Custom Expenditure Warning Banner
    if (isLimitBreached) {
      const excess = Math.round((totalExpenses - warningLimit) * 100) / 100;
      customWarningBanner.className = 'alert-banner alert-danger';
      customAlertTitle.textContent = '⚠️ Custom Expenditure Limit Breached!';
      customAlertMessage.textContent = `Your total expenditure (${formatINR(totalExpenses)}) has exceeded your manually decided limit of ${formatINR(warningLimit)} by ${formatINR(excess)}.`;
    } else {
      customWarningBanner.className = 'alert-banner alert-hidden';
    }

    // 9. Standard Budget Warning Banner
    renderStandardBudgetBanner(budget, totalExpenses, remainingBalance, percentSpent);

    // 10. Financial Health Score (PennyScore)
    updatePennyScore(budget, totalExpenses, remainingBalance, percentSpent, isLimitBreached);

    // 11. Category Breakdown Strip
    renderCategoryBreakdown(totalExpenses);

    // 12. Render Filtered & Sorted Expense List
    renderExpenses();
  }

  // Standard Budget Banner
  function renderStandardBudgetBanner(budget, totalExpenses, remainingBalance, percentSpent) {
    if (!budgetAlertBanner) return;

    if (budget === 0 && totalExpenses === 0) {
      budgetAlertBanner.className = 'alert-banner alert-hidden';
      return;
    }

    if (budget === 0 && totalExpenses > 0) {
      budgetAlertBanner.className = 'alert-banner alert-warning';
      alertIcon.textContent = '💡';
      alertTitle.textContent = 'No Monthly Budget Defined';
      alertMessage.textContent = `You have spent ${formatINR(totalExpenses)} without a set budget. Set your monthly allowance to monitor student savings.`;
      return;
    }

    if (remainingBalance < 0) {
      const overspent = Math.abs(remainingBalance);
      budgetAlertBanner.className = 'alert-banner alert-danger';
      alertIcon.textContent = '🚨';
      alertTitle.textContent = 'Budget Exceeded!';
      alertMessage.textContent = `Total spending of ${formatINR(totalExpenses)} exceeds your monthly budget of ${formatINR(budget)} by ${formatINR(overspent)}.`;
    } else if (percentSpent >= 85) {
      budgetAlertBanner.className = 'alert-banner alert-warning';
      alertIcon.textContent = '⚠️';
      alertTitle.textContent = 'Caution: Over 85% of Budget Spent!';
      alertMessage.textContent = `You have utilized ${percentSpent}% of your monthly allowance. Only ${formatINR(remainingBalance)} remaining.`;
    } else {
      budgetAlertBanner.className = 'alert-banner alert-hidden';
    }
  }

  // Financial Health Score
  function updatePennyScore(budget, totalExpenses, remainingBalance, percentSpent, isLimitBreached) {
    if (!pennyScoreValue) return;

    let score = 100;
    if (budget <= 0) {
      score = 80;
    } else if (remainingBalance < 0) {
      const overRatio = Math.abs(remainingBalance) / budget;
      score = Math.max(15, Math.round(50 - overRatio * 50));
    } else {
      // 100 down to 60 based on percentSpent
      score = Math.max(50, 100 - Math.round(percentSpent * 0.45));
      if (isLimitBreached) score -= 15;
    }

    pennyScoreValue.textContent = score;
    if (score >= 80) {
      pennyScoreValue.style.color = '#34d399';
      pennyScoreBadge.title = 'Financial Health: Excellent Student Saver!';
    } else if (score >= 60) {
      pennyScoreValue.style.color = '#fbbf24';
      pennyScoreBadge.title = 'Financial Health: Moderate Caution Advised';
    } else {
      pennyScoreValue.style.color = '#f87171';
      pennyScoreBadge.title = 'Financial Health: Critical! Spending Too Fast';
    }
  }

  // Category Breakdown Strip
  function renderCategoryBreakdown(totalExpenses) {
    if (!categoryBreakdownSection || !categoryBreakdownRow) return;

    if (totalExpenses <= 0 || state.expenses.length === 0) {
      categoryBreakdownSection.style.display = 'none';
      return;
    }

    const categoryTotals = {};
    state.expenses.forEach(item => {
      categoryTotals[item.category] = (categoryTotals[item.category] || 0) + Number(item.amount);
    });

    const sortedCats = Object.keys(categoryTotals).sort((a, b) => categoryTotals[b] - categoryTotals[a]);

    const chipsHtml = sortedCats.map(cat => {
      const amt = categoryTotals[cat];
      const pct = Math.round((amt / totalExpenses) * 100);
      const icon = CATEGORY_ICONS[cat] || '🏷️';
      return `
        <div class="category-pill-stat" title="${cat}: ${formatINR(amt)} (${pct}%)">
          <span>${icon} ${cat}</span>
          <span class="category-pill-amt">${formatINR(amt)}</span>
          <span class="category-pill-pct">${pct}%</span>
        </div>
      `;
    }).join('');

    categoryBreakdownRow.innerHTML = chipsHtml;
    categoryBreakdownSection.style.display = 'block';
  }

  // Render Expense List with Search, Filter & Sort
  function renderExpenses() {
    if (!expenseList || !emptyState) return;

    let items = state.expenses.slice();

    // 1. Filter Category
    if (state.filterCategory !== 'ALL') {
      items = items.filter(e => e.category === state.filterCategory);
    }

    // 2. Search Term
    if (state.searchTerm) {
      items = items.filter(e => 
        e.name.toLowerCase().includes(state.searchTerm) || 
        e.category.toLowerCase().includes(state.searchTerm)
      );
    }

    // 3. Sorting
    items.sort((a, b) => {
      if (state.sortBy === 'highest') return b.amount - a.amount;
      if (state.sortBy === 'lowest') return a.amount - b.amount;
      if (state.sortBy === 'oldest') return (a.timestamp || 0) - (b.timestamp || 0);
      // default newest
      return (b.timestamp || 0) - (a.timestamp || 0);
    });

    // Empty state check
    if (state.expenses.length === 0) {
      expenseList.innerHTML = '';
      emptyState.style.display = 'flex';
      emptyState.querySelector('.empty-state-title').textContent = 'No expenses recorded yet';
      emptyState.querySelector('.empty-state-text').textContent = 'Start tracking your student budget by entering your monthly allowance and adding your first expense above.';
      return;
    }

    if (items.length === 0) {
      expenseList.innerHTML = '';
      emptyState.style.display = 'flex';
      emptyState.querySelector('.empty-state-title').textContent = 'No matching expenses';
      emptyState.querySelector('.empty-state-text').textContent = 'Try adjusting your search query or category filters.';
      return;
    }

    emptyState.style.display = 'none';

    // Fast HTML template concatenation
    const html = items.map(item => {
      const icon = CATEGORY_ICONS[item.category] || '🏷️';
      return `
        <li class="expense-item" data-id="${escapeHtml(item.id)}">
          <div class="expense-left">
            <div class="expense-icon-badge" title="${escapeHtml(item.category)}">${icon}</div>
            <div class="expense-details">
              <span class="expense-name" title="${escapeHtml(item.name)}">${escapeHtml(item.name)}</span>
              <div class="expense-meta">
                <span class="expense-category-tag">${escapeHtml(item.category)}</span>
                <span>•</span>
                <span>${escapeHtml(item.date || 'Today')}</span>
              </div>
            </div>
          </div>
          <div class="expense-right">
            <span class="expense-amount">- ${formatINR(item.amount)}</span>
            <button 
              type="button" 
              class="btn-delete" 
              aria-label="Delete ${escapeHtml(item.name)}" 
              title="Delete expense"
              data-delete-id="${escapeHtml(item.id)}"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                <line x1="10" y1="11" x2="10" y2="17"></line>
                <line x1="14" y1="11" x2="14" y2="17"></line>
              </svg>
            </button>
          </div>
        </li>
      `;
    }).join('');

    expenseList.innerHTML = html;

    // Attach delete listeners
    const deleteButtons = expenseList.querySelectorAll('[data-delete-id]');
    deleteButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-delete-id');
        if (id) deleteExpense(id);
      });
    });
  }

  // Safe HTML Escaping
  function escapeHtml(str) {
    if (typeof str !== 'string') return String(str || '');
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // Boot up
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
