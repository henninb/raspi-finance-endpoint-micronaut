#!/bin/bash

# Start Development Environment for Finance Web Client

echo "🚀 Starting Finance Web Client Development Environment"
echo "=============================================="

# Check if the Micronaut API is running
echo "📡 Checking if Micronaut API is available..."
if curl -s -f http://localhost:8080/health > /dev/null 2>&1; then
    echo "✅ Micronaut API is running on port 8080"
else
    echo "❌ Micronaut API is not running on port 8080"
    echo "💡 Please start the Micronaut API first with:"
    echo "   cd .."
    echo "   ./gradlew run"
    echo ""
    echo "🔄 Starting anyway (you can start the API later)..."
fi

echo ""
echo "🌐 Starting Express Web Client..."
echo "   - Web Client: http://localhost:3000"
echo "   - API Endpoint: http://localhost:8080"
echo ""

# Start the Express app in development mode
npm run dev