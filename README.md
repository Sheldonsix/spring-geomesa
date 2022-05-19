# Spring-GeoMesa
SpringBoot + GeoMesa-HBase 分布式部署 + swagger-ui 实现时空轨迹查询。

<div align=center>
<img src="https://raw.githubusercontent.com/Sheldonsix/spring-geomesa/master/img/springboot-logo.png" width="200" /> 
</div>

<div align=center>
<img src="https://raw.githubusercontent.com/Sheldonsix/spring-geomesa/master/img/geomesa-logo.png" width="200" /> 
</div>

### 服务器部署版本

| 名称 | 版本 |
| :----: | :----: |
| Linux | Debian GNU/Linux 10 |
| JDK | 1.8.0_311 |
| Hadoop | hadoop-3.2.2 |
| ZooKeeper | apache-zookeeper-3.7.0 |
| HBase | hbase-2.4.11 |
| GeoMesa | geomesa-hbase_2.11-3.2.2 |

### 本地开发环境

| 名称 | 版本 |
| :----: | :----: |
| Windows | 11 |
| IntelliJ IDEA | 2021.3.3 |
| Visual Studio Code | 1.67.1 |
| Xshell | 6.0.0038 |
| VMware® Workstation 16 Pro | 16.2.3 |


### 测试环境

| 名称 | 版本 |
| :----: | :----: |
| Postman | 9.16.0 |

### 重要依赖及其版本

| 名称 | 版本 |
| :----: | :----: |
| spring-boot | 2.6.7 |
| swagger | 2.9.2 |
| scala | 2.11.7 |
| guava | 20.0 |
| curator | 4.3.0 |

> 依赖详情见 `pom.xml`

---

## 服务器端部署

由 Vmware 虚拟化得到三个 Linux 虚拟机。

### 虚拟机配置

1. 修改各虚拟机的机器名。
    ```
    nano /etc/hostname
    ```
    分别修改为 `master`，`slave1`，`slave2`，重启虚拟机生效。

2. 修改三台机器的 `/etc/hosts`：
    ```
    nano /etc/hosts
    # 添加以下内容，这里的 IP 地址为虚拟机的内网 IP。
    192.168.220.21 master
    192.168.220.22 slave1
    192.168.220.23 slave2
    ```

3. ssh 配置
    ```
    # 在 master 上生成一对公钥和密钥
    ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa
    # 将公钥拷贝到 master, slave1, slave2 上。
    cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
    scp ~/.ssh/id_rsa.pub root@slave1:~
    scp ~/.ssh/id_rsa.pub root@slave2:~
    # 在 slave1 机器上
    mkdir .ssh
    cat id_rsa.pub >> .ssh/authorized_keys
    chmod 755 .ssh && chmod 600 ~/.ssh/authorized_keys # 设置权限
    # 在 slave2 机器上
    mkdir .ssh
    cat id_rsa.pub >> .ssh/authorized_keys
    chmod 755 .ssh && chmod 600 ~/.ssh/authorized_keys # 设置权限
    ```
    在 `master` 机器使用 `ssh slave1` 和 `ssh slave2` 命令测试能否免密登录另外两台机器。


### 配置 JDK

1. 在 `master` 机器上，从 [Oracle官网](https://www.oracle.com/java/technologies/downloads/) 下载对应系统的 Java8， **注意**：最新版本为 Java17，但 Java8 对 HBase 支持度最好。

2. 在 `/usr/local/` 目录下新建一个目录 `jdk`，
    ```
    mkdir /usr/local/jdk
    cd /usr/local/jdk
    ```
    将 Java 安装包上传到 `jdk` 目录下，并解压
    ```
    tar -zxvf jdk-8u321-linux-x64.tar.gz
    ```

3. 解压得到新的目录 `jdk1.8.0_321`，进入该目录，并配置环境变量，
    ```
    cd jdk1.8.0_321
    nano /etc/profile
    ```
    在环境变量配置文件中新增以下代码：
    ```
    export JAVA_HOME=/usr/local/jdk/jdk1.8.0_321
    export JRE_HOME=$JAVA_HOME/jre
    export CLASSPATH=$CLASSPATH:$JAVA_HOME/lib:$JRE_HOME/lib
    export PATH=$PATH:$JAVA_HOME/bin:$JRE_HOME/bin
    ```
    键入 <kbd>Ctrl</kbd> + <kbd>x</kbd> 再输入 `y`，即可保存。

4. 输入命令 `source /etc/profile` 使配置的环境变量生效。

5. 输入命令 `java -version`，看到版本即安装成功。
    
    ![Java_version.png](https://raw.githubusercontent.com/Sheldonsix/spring-geomesa/master/img/Java_version.png)
    

6. 在另外两台机器 `slave1`、`slave2` 上进行相同的配置。
---
### Hadoop 分布式部署

1. 在 `master` 机器新建以下目录。
    ```
    mkdir /usr/local/hadoop
    mkdir /usr/local/hadoop/tmp 
    mkdir /usr/local/hadoop/var
    mkdir /usr/local/hadoop/dfs
    mkdir /usr/local/hadoop/dfs/name
    mkdir /usr/local/hadoop/dfs/data
    cd /usr/local/hadoop
    ```

2. 从 [Apache 镜像站](https://dlcdn.apache.org/hadoop/common/) 下载 Hadoop 稳定发行版，此处选择的 Hadoop 版本为最新的稳定版 `hadoop-$VERSION`，并解压。
    ```
    # 将 $VERSION 更换为相应版本，此处选择的版本是 hadoop-3.2.2，注意此处的链接具有时效性。
    wget 'https://dlcdn.apache.org/hadoop/common/hadoop-3.2.2/hadoop-3.2.2.tar.gz'
    tar -zxvf hadoop-3.2.2.tar.gz
    cd hadoop-3.2.2/
    ```

3. 修改环境配置文件 `etc/hadoop/hadoop-env.sh`，
    ```
    nano etc/hadoop/hadoop-env.sh
    ```
    添加以下内容：
    ```
    # Java 的安装路径
    export JAVA_HOME=/usr/local/jdk/jdk1.8.0_321
    # 获取 root 权限操作 Hadoop
    export HDFS_NAMENODE_USER="root"
    export HDFS_DATANODE_USER="root"
    export HDFS_SECONDARYNAMENODE_USER="root"
    export YARN_RESOURCEMANAGER_USER="root"
    export YARN_NODEMANAGER_USER="root"
    ```
    配置文件修改完成后，保存退出，输入命令 `bin/hadoop`，弹出 Hadoop 的使用文档，则说明 Hadoop 环境配置完成。

4. 修改 `etc/hadoop/core-site.xml` 文件，在 `<configuration>` 节点加入以下配置：
    ```
    <property>
        <name>hadoop.tmp.dir</name>
        <value>/usr/local/hadoop/tmp</value>
        <description>Abase for other temporary directories.</description>
    </property>
    <property>
        <name>fs.default.name</name>
        <value>hdfs://master:9000</value>
    </property>
    ```
    修改 `etc/hadoop/hdfs-site.xml` 文件，在 `<configuration>` 节点加入以下配置：
    ```
    <property>
        <name>dfs.name.dir</name>
        <value>/usr/local/hadoop/dfs/name</value>
        <description>Path on the local filesystem where theNameNode stores the namespace and transactions logs persistently.</description>
    </property>
    <property>
        <name>dfs.data.dir</name>
        <value>/usr/local/hadoop/dfs/data</value>
        <description>Comma separated list of paths on the localfilesystem of a DataNode where it should store its blocks.</description>
    </property>
    <property>
        <name>dfs.replication</name>
        <value>2</value>
    </property>
    <property>
        <name>dfs.permissions</name>
        <value>false</value>
        <description>need not permissions</description>
    </property>

    ```

5. 修改 `mapred-site.xml`，在 `<configuration>` 节点内加入以下配置：
    ```
    <property>
        <name>mapred.job.tracker</name>
        <value>master:49001</value>
    </property>
    
    <property>
        <name>mapred.local.dir</name>
        <value>/usr/local/hadoop/var</value>
    </property>
    
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>
    ```

6. 修改 `workers` 文件，加入分布式子节点的机器名：
    ```
    slave1
    slave2
    ```

7. 修改 `yarn-site.xml` 文件，在 `<configuration>` 节点内加入以下配置：
    ```
    <property>
        <name>yarn.resourcemanager.hostname</name>
        <value>master</value>
    </property>
 
    <property>
        <description>The address of the applications manager interface in the RM.</description>
        <name>yarn.resourcemanager.address</name>
        <value>${yarn.resourcemanager.hostname}:8032</value>
    </property>
 
    <property>
        <description>The address of the scheduler interface.</description>
        <name>yarn.resourcemanager.scheduler.address</name>
        <value>${yarn.resourcemanager.hostname}:8030</value>
    </property>
 
    <property>
        <description>The http address of the RM web application.</description>
        <name>yarn.resourcemanager.webapp.address</name>
        <value>${yarn.resourcemanager.hostname}:8088</value>
    </property>
 
    <property>
        <description>The https adddress of the RM web application.</description>
        <name>yarn.resourcemanager.webapp.https.address</name>
        <value>${yarn.resourcemanager.hostname}:8090</value>
    </property>
 
    <property>
        <name>yarn.resourcemanager.resource-tracker.address</name>
        <value>${yarn.resourcemanager.hostname}:8031</value>
    </property>
 
    <property>
        <description>The address of the RM admin interface.</description>
        <name>yarn.resourcemanager.admin.address</name>
        <value>${yarn.resourcemanager.hostname}:8033</value>
    </property>
 
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>
 
    <property>
        <name>yarn.scheduler.maximum-allocation-mb</name>
        <value>2048</value>
    </property>
 
    <property>
        <name>yarn.nodemanager.vmem-pmem-ratio</name>
        <value>2.1</value>
    </property>
 
    <property>
        <name>yarn.nodemanager.resource.memory-mb</name>
        <value>2048</value>
    </property>
    
    <property>
        <name>yarn.nodemanager.vmem-check-enabled</name>
        <value>false</value>
    </property>
    ```

8. 将配置文件同步到 `slave1`，`slave2` 机器中
    ```
    scp -r /usr/local/hadoop/hadoop-3.2.2 root@slave1:/usr/local/hadoop/
    scp -r /usr/local/hadoop/hadoop-3.2.2 root@slave2:/usr/local/hadoop/
    ```

6. 启动 Hadoop：
    ```
    # 格式化文件系统，只需要格式化一次，下次启动不需要格式化
    bin/hdfs namenode -format
    ```
    启动 NameNode 和 DataNode 守护进程：
    ```
    sbin/start-dfs.sh
    ```
    启动 Yarn：
    ```
    start-yarn.sh
    ```
    日志默认保存路径为 `/usr/local/hadoop/hadoop-3.2.2/logs`。现在可以打开 NameNode 的 WebUI，默认为 `http://localhost:9870`。使用以下命令停止进程
    ```
    sbin/stop-all.sh
    ```

7. 使用 `jps` 命令查看 Java 进程，此时在 `master` 应该有三个进程 `NameNode`、`SecondaryNameNode`、`ResourceManger`；在 `slave1`、`slave2` 应该有两个进程 `DataNode`、`NodeManager`。
    
---
### ZooKeeper 部署

1. 在 `/usr/local/` 路径下新建目录 `zookeeper`：
    ```
    mkdir /usr/local/zookeeper
    cd /usr/local/zookeeper
    ```

2. 在 [Apache 镜像站](https://zookeeper.apache.org/releases.html) 下载最新的稳定版，此处下载的 ZooKeeper 版本为 `Apache ZooKeeper 3.6.3`，**注意**，此处需要下载的是 ZooKeeper 的可执行版本(apache-zookeeper-3.6.3-bin)，而并不是源码。下载完成之后解压：
    ```
    wget 'https://dlcdn.apache.org/zookeeper/zookeeper-3.6.3/apache-zookeeper-3.6.3-bin.tar.gz'
    tar -xvzf apache-zookeeper-3.6.3-bin.tar.gz
    cd apache-zookeeper-3.6.3-bin
    ```

3. 单机操作。创建 `conf/zoo.cfg` 文件，文件内容如下：
    ```
    # tickTime：ZooKeeper 使用的基本时间单位，以毫秒为单位。
    tickTime=2000
    # dataDir：存储内存中数据库快照的位置，以及更新数据库的日志。
    dataDir=/usr/local/zookeeper/apache-zookeeper-3.6.3-bin/data  # 此处为任意一个空目录即可
    # clientPort：侦听客户端连接的端口
    clientPort=2181
    ```
    保存退出之后，可以启动 ZooKeeper：
    ```
    bin/zkServer.sh start
    ```

4. 连接到 ZooKeeper：
    ```
    bin/zkCli.sh -server 127.0.0.1:2181
    ```
    输入 `quit` 退出 ZooKeeper 客户端。
    
---
### HBase 部署

1. 在 `/usr/local/` 路径下新建目录 `hbase`：
    ```
    mkdir /usr/local/hbase
    cd /usr/local/hbase
    ```

2. 在 [Apache 镜像站](https://dlcdn.apache.org/hbase/) 下载 HBase 镜像，此处选择的是最新的稳定版本 `hbase-2.4.9-bin`。下载完成后解压。
    ```
    wget 'https://dlcdn.apache.org/hbase/stable/hbase-2.4.9-bin.tar.gz'
    tar -zxvf hbase-2.4.9-bin.tar.gz
    cd hbase-2.4.9
    ```

3. 确保在启动 HBase 之前，已经设置了 `JAVA_HOME` 环境变量。修改 `conf/HBase-env.sh` 文件，添加以下内容：
    ```
    export JAVA_HOME=/usr/local/jdk/jdk1.8.0_311  # Java 的安装路径
    ```
    使用以下命令启动 HBase：
    ```
    bin/start-hbase.sh
    ```
    使用 `jps` 来查看有一个名叫 `HMaster` 的进程，默认的 WebUI 为 `http://localhost:16010`。

4. 连接到 HBase：
    ```
    ./bin/hbase shell
    ```
    使用 `help` 命令来查看 HBase Shell 的一些基本使用信息，使用 `quit` 命令退出 HBase Shell。

5. 使用以下命令停止所有的 HBase 守护进程：
    ```
    ./bin/stop-hbase.sh
    ```

---
### GeoMesa 部署

1.  在 `/usr/local/` 路径下新建目录 `geomesa`：
    ```
    mkdir /usr/local/geomesa
    cd /usr/local/geomesa
    ``` 
    从 [GitHub](https://github.com/locationtech/geomesa/releases/) 下载编译完成的二进制文件（bin），此处选的的是 `geomesa-hbase_2.11-3.2.2-bin.tar.gz`，**注意**：geomesa 有两个版本号，前面的 `2.11` 是它支持的 scala 的版本号，后面的才是它本身的版本号。下载完成之后进行解压：
    ```
    wget 'https://github.com/locationtech/geomesa/releases/download/geomesa-3.2.2/geomesa-hbase_2.11-3.2.2-bin.tar.gz'
    tar -zxvf geomesa-hbase_2.11-3.2.2-bin.tar.gz
    cd geomesa-hbase_2.11-3.2.2
    ```

2. 修改配置信息。在路径 `conf/geomesa-env.sh` 中添加以下环境变量：
    ```
    export HADOOP_HOME=/usr/local/hadoop/hadoop-3.3.1
    export HBASE_HOME=/usr/local/hbase/hbase-2.4.9
    export GEOMESA_HBASE_HOME=/usr/local/geomesa/geomesa-hbase_2.11-3.2.2
    export PATH="${PATH}:${GEOMESA_HBASE_HOME}/bin" 
    ```
    **注意**：配置 GeoMesa 的环境变量是 `GEOMESA_HBASE_HOME`。

3. 部署 GeoMesa-HBase。将 GeoMesa 的 runtime JAR 包拷贝到 HBase 的库目录下。首先启动 Hadoop：
    ```
    # 首先启动 Hadoop
    /usr/local/hadoop/hadoop-3.3.1/sbin/start-dfs.sh
    ```
    然后需要修改 HBase 的配置文件，文件路径为 `/usr/local/hbase/hbase-2.4.9/conf/hbase-site.xml`，在 `<configuration>...</configuration>` 之间添加以下内容：
    ```
    <property>
        <name>hbase.rootdir</name>
        <value>hdfs://localhost:9000/hbase</value>
    </property>
    <property>
        <name>hbase.zookeeper.property.dataDir</name>
        <value>/usr/local/hbase/hbase-2.4.9/zookeeper</value>
    </property>
    <property>
        <name>hbase.cluster.distributed</name>
        <value>true</value>
    </property>
    <property>
        <name>hbase.tmp.dir</name>
        <value>/usr/local/hbase/hbase-2.4.9/tmp</value>
    </property>
    <property>
        <name>hbase.unsafe.stream.capability.enforce</name>
        <value>false</value>
    </property>
    ```
    然后修改 HBase 的运行环境配置，路径为 `/usr/local/hbase/hbase-2.4.9/conf/hbase-env.sh`，添加以下内容：
    ```
    export HBASE_CLASSPATH=/usr/local/hadoop/hadoop-3.3.1
    export HBASE_MANAGES_ZK=true
    ```
    最后使用以下命令拷贝到 HDFS 中：
    ```
    # ${hbase.rootdir} 参考 HBase 的配置文件 hbase-site.xml
    hadoop fs -put ${GEOMESA_HBASE_HOME}/dist/hbase/geomesa-hbase-distributed-runtime-hbase2_2.11-3.2.2.jar hdfs://localhost:9000/hbase/lib
    ```

4. 注册协处理器。为了使 HBase 在运行时能够访问到 `geomesa-hbase-distributed-runtime` 的 jar 包，需要在 HBase 的配置文件 `hbase-site.xml` 添加以下内容：
    ```
    <property>
        <name>hbase.coprocessor.user.region.classes</name>
        <value>org.locationtech.geomesa.hbase.coprocessor.GeoMesaCoprocessor</value>
    </property>
    ```

5. 设置命令行工具，将 HBase 配置文件 `hbase-site.xml` 打包进 `geomesa-hbase-datastore_2.11-3.2.2.jar` 中：
    ```
    zip -r /usr/local/geomesa/geomesa-hbase_2.11-3.2.2/lib/geomesa-hbase-datastore_2.11-3.2.2.jar /usr/local/hbase/hbase-2.4.9/conf/hbase-site.xml
    ```

6. 重新启动 Hadoop、HBase、ZooKeeper，然后查看 geomesa-hbase 版本：
    ```
    # 重新启动 HBase、Hadoop、ZooKeeper，注意关闭启动的顺序
    /usr/local/hbase/hbase-2.4.9/bin/stop-hbase.sh
    /usr/local/hadoop/hadoop-3.3.1/sbin/stop-all.sh 
    /usr/local/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkServer.sh stop
    /usr/local/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkServer.sh start
    /usr/local/hadoop/hadoop-3.3.1/sbin/start-all.sh 
    /usr/local/hbase/hbase-2.4.9/bin/start-hbase.sh
    ```
