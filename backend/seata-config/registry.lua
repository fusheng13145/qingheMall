-- Seata Registry Configuration for Docker
config {
  type = "file"

  file {
    naming = "lua"
  }
}

registry {
  type = "file"
  file {
    name = "file.conf"
  }
}

server {
  vgroupMapping.my_test_tx_group = "default"
  enableDegrade = false
  recovery = true
  failover = true
}

transport {
  type = "TCP"
  server = "NIO"
  heartbeat = true
  init = true
  session = true
}

client {
  async = {
    queueSize = 1024
    keepAlive = true
    concurrency = 10
  }
  report = {
    enable = true
    interval = 1000
  }
}
