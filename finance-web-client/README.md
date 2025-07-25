# Finance Web Client

A professional Express.js web application that provides a beautiful interface for the Raspi Finance Endpoint Micronaut API.

## Features

- **Account Overview Dashboard**: View all accounts in a professional table layout
- **Account Detail Pages**: Click through to see detailed transaction history for each account
- **Professional UI**: Modern Bootstrap 5 design with custom styling
- **Responsive Design**: Works great on desktop, tablet, and mobile devices
- **Search & Filter**: Real-time search and filtering capabilities
- **Real-time Statistics**: Account summaries with visual indicators
- **Error Handling**: Graceful error handling with user-friendly messages

## Screenshots

### Account List View
- Professional table layout showing all accounts
- Account type badges with color coding
- Active/inactive status indicators
- Search functionality
- Statistics cards showing totals

### Account Detail View
- Transaction history with running balance
- Credit/debit indicators with color coding
- Transaction filtering and search
- Account summary statistics
- Professional breadcrumb navigation

## Tech Stack

- **Backend**: Express.js + Node.js
- **Frontend**: Bootstrap 5 + Font Awesome icons
- **Template Engine**: EJS
- **API Client**: Axios
- **Rate Limiting**: Express Rate Limit
- **Development**: Nodemon for hot reloading

## Getting Started

### Prerequisites

- Node.js (v14 or higher)
- NPM or Yarn
- Running Micronaut API server (on port 8080 by default)

### Installation

1. Navigate to the finance-web-client directory:
   ```bash
   cd finance-web-client
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the application:
   ```bash
   # Development mode (with auto-restart)
   npm run dev
   
   # Production mode
   npm start
   ```

4. Open your browser and navigate to:
   ```
   http://localhost:3000
   ```

### Configuration

The application can be configured using environment variables:

- `PORT`: Server port (default: 3000)
- `API_BASE_URL`: Micronaut API base URL (default: http://localhost:8080)
- `NODE_ENV`: Environment mode (development/production)

Example:
```bash
PORT=4000 API_BASE_URL=http://api.mycompany.com npm start
```

## API Endpoints

The web client consumes the following Micronaut API endpoints:

- `GET /account` - Fetch all accounts
- `GET /account/{id}` - Fetch account by ID
- `GET /transaction/account/{accountNameOwner}` - Fetch transactions for an account
- `GET /account/summary/{accountNameOwner}` - Fetch account summary

## Project Structure

```
finance-web-client/
├── server.js              # Main Express server
├── package.json           # Dependencies and scripts
├── views/                 # EJS templates
│   ├── accounts.ejs       # Account list page
│   ├── account-detail.ejs # Account detail page
│   └── error.ejs          # Error page
├── public/                # Static assets
│   ├── css/
│   │   └── custom.css     # Custom styles
│   └── js/
│       └── app.js         # Client-side JavaScript
└── README.md              # This file
```

## Features Detail

### Account List Page
- **Professional Table**: Clean, modern table design with hover effects
- **Account Statistics**: Overview cards showing total accounts, active accounts, and account types
- **Search Functionality**: Real-time search across account names and details
- **Account Type Badges**: Color-coded badges for different account types (checking, savings, credit, etc.)
- **Status Indicators**: Clear active/inactive status with appropriate icons
- **Responsive Design**: Looks great on all screen sizes

### Account Detail Page
- **Transaction History**: Complete transaction listing with running balance calculation
- **Visual Indicators**: Color-coded icons for credits (green) and debits (red)
- **Transaction Statistics**: Summary cards showing transaction counts and net balance
- **Advanced Filtering**: Filter by transaction type (credits/debits) plus text search
- **Professional Breadcrumbs**: Easy navigation back to account list
- **Date Formatting**: User-friendly date and time display

### Design Philosophy
- **User Experience**: Clean, intuitive interface that's easy to navigate
- **Professional Appearance**: Suitable for business and personal use
- **Performance**: Fast loading with efficient API calls
- **Accessibility**: Proper contrast, keyboard navigation, screen reader friendly
- **Mobile First**: Responsive design that works on all devices

## Development

### Code Structure
- **Modular Design**: Separate API client class for clean code organization
- **Error Handling**: Comprehensive error handling at all levels
- **Rate Limiting**: Built-in protection against API abuse
- **Security**: Input validation and secure headers

### Adding New Features
1. Add new routes in `server.js`
2. Create corresponding EJS templates in `views/`
3. Add any new styles to `public/css/custom.css`
4. Add client-side functionality to `public/js/app.js`

## Troubleshooting

### Common Issues

1. **API Connection Failed**
   - Ensure the Micronaut API is running on the specified port
   - Check the `API_BASE_URL` environment variable
   - Verify network connectivity

2. **No Accounts Displayed**
   - Check the Micronaut API `/account` endpoint
   - Look at browser console for error messages
   - Verify API returns valid JSON

3. **Styling Issues**
   - Clear browser cache
   - Check if Bootstrap CSS is loading properly
   - Verify custom.css is being served

### Logs
The application logs important events to the console:
- Server startup information
- API request errors
- Client connection issues

## License

MIT License - see the main project for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Support

For issues related to the web client, please check:
1. This README file
2. Browser console for client-side errors
3. Server logs for backend issues
4. API documentation for endpoint details