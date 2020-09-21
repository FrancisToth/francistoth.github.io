if http-server -p 8000 ./ ; then
    echo "HTTP SERVER EXECUTED"
else
    echo ""
    echo "[ERROR] UNABLE TO EXECUTE NODE HTTP SERVER. FALLING BACK TO UNRELIABLE PYTHON SERVER."
    echo "THE PYTHON SERVER IS OK FOR LOCAL USE, BUT UNSUITABLE FOR BUILDING FINAL ARTIFACTS."
    echo ""
    echo "TO INSTALL THE NODE SERVER"
    echo "ON MAC"
    echo "> brew install node"
    echo "> npm install -g http-server"
    echo ""
    
    python -m SimpleHTTPServer 8000
fi