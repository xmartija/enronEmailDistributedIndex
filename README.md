# enronEmailDistributedIndex 
Use Docker Machine + Swarm + Compose + Solr to distribute the indexing of the Enron  PST Email Dataset

COORDINATOR:
docker-machine create -d virtualbox coordinator

docker $(docker-machine config coordinator) run -d -p 172.17.42.1:53:53 -p 172.17.42.1:53:53/udp -p 8300:8300 -p 8301:8301 -p 8301:8301/udp -p 8302:8302 -p 8302:8302/udp -p 8400:8400 -p 8500:8500 --name consul progrium/consul -server -bootstrap-expect 1 -advertise $(docker-machine ip coordinator)


MASTER
docker-machine create -d virtualbox --swarm --swarm-master --swarm-discovery consul://$(docker-machine ip coordinator):8500 master-node

docker $(docker-machine config master-node) run -d -p 172.17.42.1:53:53 -p 172.17.42.1:53:53/udp -p 8301:8301 -p 8301:8301/udp -p 8500:8500 --name consul progrium/consul -join $(docker-machine ip coordinator) -advertise $(docker-machine ip master-node)

//Registrator Note extra / for volumes in windows need // to refer to the host's filesystem
docker $(docker-machine config master-node) run -d -v //var/run/docker.sock:/tmp/docker.sock -h registrator --name registrator gliderlabs/registrator -ip $(docker-machine ip master-node) consul://$(docker-machine ip master-node):8500 


  


SLAVES
docker-machine create -d virtualbox --swarm --swarm-discovery consul://$(docker-machine ip coordinator):8500 node-1

docker $(docker-machine config node-1) run -d -p 172.17.42.1:53:53 -p 172.17.42.1:53:53/udp -p 8301:8301 -p 8301:8301/udp -p 8500:8500 --name consul progrium/consul -join $(docker-machine ip coordinator) -advertise $(docker-machine ip node-1)

//Registrator Note extra / for volumes in windows need // to refer to the host's filesystem
docker $(docker-machine config node-1) run -d -v //var/run/docker.sock:/tmp/docker.sock -h registrator --name registrator gliderlabs/registrator -ip $(docker-machine ip node-1) consul://$(docker-machine ip node-1):8500



PROXY
//HA proxy. at least one node, any node
docker $(docker-machine config --swarm master-node) run -d -h rest --name=rest -e SERVICE_NAME=rest --dns 172.17.42.1 -p 80:80 -p 1936:1936 sirile/haproxy -consul=$(docker-machine ip coordinator):8500

DOCKER-COMPOSE
docker-machine create -d virtualbox --swarm --swarm-discovery consul://$(docker-machine ip coordinator):8500 composer
  88  docker-machine create -d virtualbox --swarm --swarm-discovery consul://$(docker-machine ip coordinator):8500 composer
  89  docker $(docker-machine config composer) ps
  90  docker $(docker-machine config composer) build -t docker-compose github.com/docker/compose
  91  history
alias docker-compose='docker $(docker-machine config composer)  run --rm -ti -v /var/run/docker.sock:/var/run/docker.sock -v `pwd`:`pwd` -w `pwd` docker-compose'

alias docker-compose='docker $(docker-machine config composer)  run --rm -ti -v //var/run/docker.sock:/var/run/docker.sock -v /`pwd`:`pwd` -w /`pwd` docker-compose'


SOLRCORES
