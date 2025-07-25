const express = require('express');
const axios = require('axios');
const path = require('path');
const rateLimit = require('express-rate-limit');

const app = express();
const PORT = process.env.PORT || 3000;
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});

app.use(limiter);

// Set view engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Static files
app.use(express.static(path.join(__dirname, 'public')));

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// API client helper
class FinanceAPIClient {
  constructor(baseURL) {
    this.baseURL = baseURL;
    this.client = axios.create({
      baseURL: baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    });
  }

  async getAccounts() {
    try {
      const response = await this.client.get('/account/select/active');
      return response.data;
    } catch (error) {
      console.error('Error fetching accounts:', error.message);
      throw new Error('Failed to fetch accounts');
    }
  }

  async getAccountById(accountNameOwner) {
    try {
      const response = await this.client.get(`/account/select/${accountNameOwner}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching account ${accountNameOwner}:`, error.message);
      throw new Error('Failed to fetch account details');
    }
  }

  async getTransactionsByAccount(accountNameOwner) {
    try {
      const response = await this.client.get(`/transaction/account/select/${encodeURIComponent(accountNameOwner)}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching transactions for ${accountNameOwner}:`, error.message);
      throw new Error('Failed to fetch transactions');
    }
  }

  async getAccountSummary(accountNameOwner) {
    try {
      const response = await this.client.get(`/transaction/account/totals/${encodeURIComponent(accountNameOwner)}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching account summary for ${accountNameOwner}:`, error.message);
      return null;
    }
  }
}

const apiClient = new FinanceAPIClient(API_BASE_URL);

// Routes

// Home page - redirect to accounts
app.get('/', (req, res) => {
  res.redirect('/accounts');
});

// Accounts list page
app.get('/accounts', async (req, res) => {
  try {
    const accounts = await apiClient.getAccounts();
    res.render('accounts', { 
      title: 'Finance Dashboard - Accounts',
      accounts: accounts,
      error: null
    });
  } catch (error) {
    res.render('accounts', { 
      title: 'Finance Dashboard - Accounts',
      accounts: [],
      error: error.message
    });
  }
});

// Account detail page with transactions
app.get('/account/:accountNameOwner', async (req, res) => {
  const { accountNameOwner } = req.params;
  
  try {
    const [transactions, accountSummary] = await Promise.allSettled([
      apiClient.getTransactionsByAccount(accountNameOwner),
      apiClient.getAccountSummary(accountNameOwner)
    ]);

    const transactionsData = transactions.status === 'fulfilled' ? transactions.value : [];
    const summaryData = accountSummary.status === 'fulfilled' ? accountSummary.value : null;

    res.render('account-detail', {
      title: `Account Details - ${decodeURIComponent(accountNameOwner)}`,
      accountName: decodeURIComponent(accountNameOwner),
      transactions: transactionsData,
      summary: summaryData,
      error: transactions.status === 'rejected' ? transactions.reason.message : null
    });
  } catch (error) {
    res.render('account-detail', {
      title: `Account Details - ${decodeURIComponent(accountNameOwner)}`,
      accountName: decodeURIComponent(accountNameOwner),
      transactions: [],
      summary: null,
      error: error.message
    });
  }
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).render('error', {
    title: 'Error',
    message: 'Something went wrong!',
    error: process.env.NODE_ENV === 'development' ? err : {}
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).render('error', {
    title: 'Page Not Found',
    message: 'The page you are looking for does not exist.',
    error: {}
  });
});

app.listen(PORT, () => {
  console.log(`🚀 Finance Web Client running on http://localhost:${PORT}`);
  console.log(`📊 API Base URL: ${API_BASE_URL}`);
  console.log(`🌍 Environment: ${process.env.NODE_ENV || 'development'}`);
});

module.exports = app;