# K8S

## 安装

```bash
#修改主机名
#注意： 由于 Kubernetes 暂不支持大写 NodeName， hostname 中包含大写字母将导致后续安装过程无法正常结束
hostnamectl set-hostname ubuntu-k8s
#修改时区
timedatectl set-timezone Asia/Shanghai
#修改ip
nano /etc/netplan/00-installer-config.yaml

network:
  ethernets:
    ens33:
      dhcp4: false
      addresses: [192.168.2.231/24]
      optional: true
      gateway4: 192.168.2.1
      nameservers:
        addresses: [192.168.2.1,114.114.114.114]
  version: 2
  
netplan apply

#禁用防火墙
ufw disable
#修改hosts
nano /etc/hosts
192.168.2.231 ubuntu-k8s
192.168.2.232 ubuntu-k8s2
192.168.2.233 ubuntu-kus3


#关闭 swap：
#临时
swapoff -a 
#永久
sed -ri 's/.*swap.*/#&/' /etc/fstab 
#验证，swap 必须为 0；
free -g 




#允许 iptables 检查桥接流量
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
EOF

cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF
sysctl --system


#更新
apt update 
apt list --upgradable
apt upgrade
apt autoremove



#卸载ubuntu安装时用snap安装的docker
snap remove docker
#apt remove docker docker-engine docker.io containerd runc

apt install \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

apt update
apt install docker-ce docker-ce-cli containerd.io
 
#配置 cgroup 驱动
#https://kubernetes.io/zh/docs/tasks/administer-cluster/kubeadm/configure-cgroup-driver/
#systemd与cgroupfs
#https://www.jianshu.com/p/8a62750c0eef

mkdir /etc/docker
cat <<EOF | sudo tee /etc/docker/daemon.json
{
  "exec-opts": ["native.cgroupdriver=systemd"],
  "log-driver": "json-file",
  "registry-mirrors": ["https://2wfkcpg0.mirror.aliyuncs.com","https://dockerhub.azk8s.cn","https://reg-mirror.qiniu.com"],
  "log-opts": {
    "max-size": "100m"
  },
  "storage-driver": "overlay2"
}
EOF

systemctl enable docker
systemctl daemon-reload
systemctl restart docker


#kubernetes阿里源安装
apt update && apt install -y apt-transport-https
curl https://mirrors.aliyun.com/kubernetes/apt/doc/apt-key.gpg | apt-key add - 
cat <<EOF >/etc/apt/sources.list.d/kubernetes.list
deb https://mirrors.aliyun.com/kubernetes/apt/ kubernetes-xenial main
EOF
apt update
apt install -y kubelet kubeadm kubectl
#锁定版本
apt-mark hold kubelet kubeadm kubectl

#开机自启
systemctl enable kubelet && systemctl start kubelet


-------------------------------------------------------------------------------------------------



#master节点初始化

kubelet --version

#下载用到的image，也可用来测试是否联通网络
kubeadm config images pull
#docker images

kubeadm init \
--apiserver-advertise-address 192.168.2.231 \
--kubernetes-version v1.23.5 \
--pod-network-cidr 10.244.0.0/16 \
--service-cidr 10.96.0.0/16

#根据提示运行
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

#安装Pod网络插件 Calico复杂 Flannel简单
kubectl apply -f https://raw.githubusercontent.com/flannel-io/flannel/master/Documentation/kube-flannel.yml


#查看所有命名空间下的pod,确保Flannel在运行状态
kubectl get pods --all-namespaces

#查看master节点为Ready状态
kubectl get nodes

#在其他机器加入节点命令（kubeadm init后复制下来，可能会过期需要重新生成）
kubeadm join 192.168.2.231:6443 --token 0hjxdi.una90chh8zk9ksw3 \
--discovery-token-ca-cert-hash sha256:f1379b2d1fcbb3e3e83c2e20e0998a00433d48b6609c1d439da52c77b4025abd
 
#监控 pod 进度.等 3-10 分钟，完全都是 running 以后继续
watch kubectl get pod -n kube-system -o wide 



#Ingress 根据域名访问service https://kubernetes.github.io/ingress-nginx/deploy/#quick-start
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.1.2/deploy/static/provider/cloud/deploy.yaml

#部署 Dashboard UI 功能过于简单
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.5.0/aio/deploy/recommended.yaml

#Kuboard 开源,集群要求不高

#KubeSphere 功能强大

```

## 简单使用

```bash
#部署一个tomcat
kubectl create deployment tomcat6 --image=tomcat:6.0.53-jre8
#查看状态
kubectl get all -o wide

#暴露端口，Pod的80端口映射容器的8080端口，生成一个service并用随机端口代理Pod的80端口
kubectl expose deployment tomcat6 --port=80 --target-port=8080 --type=NodePort
#查看service
kubectl get svc -o wide

#动态扩容3份
kubectl scale --replicas=3 deployment tomcat6


kubectl get all
#删除整个部署，pod被删除，service还在
kubectl delete deployment.apps/tomcat6
kubectl delete service/tomcat6
```

[**资源类型**](https://kubernetes.io/zh/docs/reference/kubectl/overview/#%E8%B5%84%E6%BA%90%E7%B1%BB%E5%9E%8B)

[**格式化输出**](https://kubernetes.io/zh/docs/reference/kubectl/overview/#%E6%A0%BC%E5%BC%8F%E5%8C%96%E8%BE%93%E5%87%BA)

[**常用操作**](https://kubernetes.io/zh/docs/reference/kubectl/overview/#%E7%A4%BA%E4%BE%8B-%E5%B8%B8%E7%94%A8%E6%93%8D%E4%BD%9C)



# Kubesphere

[官方安装教程](https://kubesphere.io/zh/docs/installing-on-linux/introduction/intro/)

使用[KubeKey](https://github.com/kubesphere/kubekey/)安装K8S非常方便。省去以上大部分步骤。

```bash
#同步时间
timedatectl set-timezone Asia/Shanghai
#安装依赖
apt install ipset
apt install ebtables
apt install conntrack
apt install socat
#下载KubeKey
curl -sfL https://get-kk.kubesphere.io | VERSION=v1.2.1 sh -
#或者直接下载二进制文件放入root目录
#https://github.com/kubesphere/kubekey/releases/
chmod +x kk

#创建配置文件
./kk create config --with-kubernetes v1.21.5 --with-kubesphere v3.2.1
#修改里面的 spec:-》hosts ；roleGroups
#开始安装
export KKZONE=cn
./kk create cluster -f config-sample.yaml
#查看日志
kubectl logs -n kubesphere-system $(kubectl get pod -n kubesphere-system -l app=ks-install -o jsonpath='{.items[0].metadata.name}') -f
#查看pod是否都准备好
kubectl get pod --all-namespaces
```



config-sample.yaml 例子

```yaml
 
apiVersion: kubekey.kubesphere.io/v1alpha2
kind: Cluster
metadata:
  name: sample
spec:
  hosts:
  - {name: k8s-master-1, address: 192.168.2.71, internalAddress: 192.168.2.71, user: root, password: "123123"}
  - {name: k8s-node-1, address: 192.168.2.72, internalAddress: 192.168.2.72, user: root, password: "123123"}
  - {name: k8s-node-2, address: 192.168.2.73, internalAddress: 192.168.2.73, user: root, password: "123123"}
  roleGroups:
    etcd:
    - k8s-master-1
    control-plane: 
    - k8s-master-1
    worker:
    - k8s-node-1
    - k8s-node-2
  controlPlaneEndpoint:
    ## Internal loadbalancer for apiservers 
    # internalLoadbalancer: haproxy

    domain: lb.kubesphere.local
    address: ""
    port: 6443
  kubernetes:
    version: v1.21.5
    clusterName: cluster.local
  network:
    plugin: calico
    kubePodsCIDR: 10.233.64.0/18
    kubeServiceCIDR: 10.233.0.0/18
    ## multus support. https://github.com/k8snetworkplumbingwg/multus-cni
    multusCNI:
      enabled: false
  registry:
    plainHTTP: false
    privateRegistry: ""
    namespaceOverride: ""
    registryMirrors: []
    insecureRegistries: []
  addons: []



---
apiVersion: installer.kubesphere.io/v1alpha1
kind: ClusterConfiguration
metadata:
  name: ks-installer
  namespace: kubesphere-system
  labels:
    version: v3.2.1
spec:
  persistence:
    storageClass: ""
  authentication:
    jwtSecret: ""
  local_registry: ""
  namespace_override: ""
  # dev_tag: ""
  etcd:
    monitoring: false
    endpointIps: localhost
    port: 2379
    tlsEnable: true
  common:
    core:
      console:
        enableMultiLogin: true
        port: 30880
        type: NodePort
    # apiserver:
    #  resources: {}
    # controllerManager:
    #  resources: {}
    redis:
      enabled: false
      volumeSize: 2Gi
    openldap:
      enabled: false
      volumeSize: 2Gi
    minio:
      volumeSize: 20Gi
    monitoring:
      # type: external
      endpoint: http://prometheus-operated.kubesphere-monitoring-system.svc:9090
      GPUMonitoring:
        enabled: false
    gpu:
      kinds:         
      - resourceName: "nvidia.com/gpu"
        resourceType: "GPU"
        default: true
    es:
      # master:
      #   volumeSize: 4Gi
      #   replicas: 1
      #   resources: {}
      # data:
      #   volumeSize: 20Gi
      #   replicas: 1
      #   resources: {}
      logMaxAge: 7
      elkPrefix: logstash
      basicAuth:
        enabled: false
        username: ""
        password: ""
      externalElasticsearchHost: ""
      externalElasticsearchPort: ""
  alerting:
    enabled: true
    # thanosruler:
    #   replicas: 1
    #   resources: {}
  auditing:
    enabled: false
    # operator:
    #   resources: {}
    # webhook:
    #   resources: {}
  devops:
    enabled: true
    jenkinsMemoryLim: 2Gi
    jenkinsMemoryReq: 1500Mi
    jenkinsVolumeSize: 8Gi
    jenkinsJavaOpts_Xms: 512m
    jenkinsJavaOpts_Xmx: 512m
    jenkinsJavaOpts_MaxRAM: 2g
  events:
    enabled: false
    # operator:
    #   resources: {}
    # exporter:
    #   resources: {}
    # ruler:
    #   enabled: true
    #   replicas: 2
    #   resources: {}
  logging:
    enabled: false
    containerruntime: docker
    logsidecar:
      enabled: true
      replicas: 2
      # resources: {}
  metrics_server:
    enabled: false
  monitoring:
    storageClass: ""
    # kube_rbac_proxy:
    #   resources: {}
    # kube_state_metrics:
    #   resources: {}
    # prometheus:
    #   replicas: 1
    #   volumeSize: 20Gi
    #   resources: {}
    #   operator:
    #     resources: {}
    #   adapter:
    #     resources: {}
    # node_exporter:
    #   resources: {}
    # alertmanager:
    #   replicas: 1
    #   resources: {}
    # notification_manager:
    #   resources: {}
    #   operator:
    #     resources: {}
    #   proxy:
    #     resources: {}
    gpu:
      nvidia_dcgm_exporter:
        enabled: false
        # resources: {}
  multicluster:
    clusterRole: none 
  network:
    networkpolicy:
      enabled: false
    ippool:
      type: none
    topology:
      type: none
  openpitrix:
    store:
      enabled: false
  servicemesh:
    enabled: false
  kubeedge:
    enabled: false   
    cloudCore:
      nodeSelector: {"node-role.kubernetes.io/worker": ""}
      tolerations: []
      cloudhubPort: "10000"
      cloudhubQuicPort: "10001"
      cloudhubHttpsPort: "10002"
      cloudstreamPort: "10003"
      tunnelPort: "10004"
      cloudHub:
        advertiseAddress:
          - ""
        nodeLimit: "100"
      service:
        cloudhubNodePort: "30000"
        cloudhubQuicNodePort: "30001"
        cloudhubHttpsNodePort: "30002"
        cloudstreamNodePort: "30003"
        tunnelNodePort: "30004"
    edgeWatcher:
      nodeSelector: {"node-role.kubernetes.io/worker": ""}
      tolerations: []
      edgeWatcherAgent:
        nodeSelector: {"node-role.kubernetes.io/worker": ""}
        tolerations: []


```

# Mysql主从

master配置

```pro
[client]
default-character-set=utf8mb4
[mysql]
default-character-set=utf8mb4
[mysqld]
init_connect='SET NAMES utf8mb4'
collation-server=utf8mb4_unicode_ci
skip-character-set-client-handshake = true
skip-name-resolve
secure_file_priv=/var/lib/mysql

server_id=1
log-bin=mysql-bin
read-only=0
binlog-do-db=mahumall_ums
binlog-do-db=mahumall_pms
binlog-do-db=mahumall_oms
binlog-do-db=mahumall_sms
binlog-do-db=mahumall_wms
binlog-do-db=mahumall_admin
replicate-ignore-db=mysql
replicate-ignore-db=sys
replicate-ignore-db=information_schema
replicate-ignore-db=performance_schema
```

slaver配置

```
[client]
default-character-set=utf8mb4
[mysql]
default-character-set=utf8mb4
[mysqld]
init_connect='SET NAMES utf8mb4'
collation-server=utf8mb4_unicode_ci
skip-character-set-client-handshake = true
skip-name-resolve
secure_file_priv=/var/lib/mysql

server_id=2
log-bin=mysql-bin
read-only=1
binlog-do-db=mahumall_ums
binlog-do-db=mahumall_pms
binlog-do-db=mahumall_oms
binlog-do-db=mahumall_sms
binlog-do-db=mahumall_wms
binlog-do-db=mahumall_admin
replicate-ignore-db=mysql
replicate-ignore-db=sys
replicate-ignore-db=information_schema
replicate-ignore-db=performance_schema
