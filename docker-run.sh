echo "Building container..."
docker build -t calefaction .
echo "Stopping all containers ..."
docker stop $(docker ps -q)
echo "Removing all containers ..."
docker rm $(docker ps -aq)
echo "Running container..."
docker run --restart=always -d --env-file=env_vars -p 8080:8080 calefaction
echo "Pruning..."
docker image prune -f