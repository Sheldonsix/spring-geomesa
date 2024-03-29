# Spring-GeoMesa
SpringBoot + GeoMesa-HBase 实现时空轨迹分布式查询。

<img src="https://raw.githubusercontent.com/Sheldonsix/spring-geomesa/master/img/springboot-logo.png" width="200" /> 

<img src="https://raw.githubusercontent.com/Sheldonsix/spring-geomesa/master/img/geomesa-logo.png" width="200" /> 


---
## 使用方法
1. 将项目拷贝到本地，使用 IDEA 打开项目文件夹；
2. 完成服务器端的部署；
3. 更改 `pom.xml` 中依赖的版本，将 `MilitaryShipTraceController.java` 和 `InfraredRayController.java` 中 `hbase.zookeepers` 的 `master,slave1,slave2` 更改为自己的 `zookeeper` 的 IP 地址；
4. 等待依赖下载完成；
5. 运行程序。

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

| 名称 |   版本   |
| :----: |:------:|
| Postman | 9.16.0 |
| JMeter | 5.4.3  |

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
    $ nano /etc/hostname
    ```
    分别修改为 `master`，`slave1`，`slave2`，重启虚拟机生效。

2. 修改三台机器的 `/etc/hosts`：
    ```
    $ nano /etc/hosts
    # 添加以下内容，这里的 IP 地址为虚拟机的内网 IP。
    192.168.220.21 master
    192.168.220.22 slave1
    192.168.220.23 slave2
    ```

3. ssh 配置
    ```
    # 在 master 上生成一对公钥和密钥
    $ ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa
    # 将公钥拷贝到 master, slave1, slave2 上。
    $ cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
    $ scp ~/.ssh/id_rsa.pub root@slave1:~
    $ scp ~/.ssh/id_rsa.pub root@slave2:~
    # 在 slave1 机器上
    $ mkdir .ssh
    $ cat id_rsa.pub >> .ssh/authorized_keys
    $ chmod 755 .ssh && chmod 600 ~/.ssh/authorized_keys # 设置权限
    # 在 slave2 机器上
    $ mkdir .ssh
    $ cat id_rsa.pub >> .ssh/authorized_keys
    $ chmod 755 .ssh && chmod 600 ~/.ssh/authorized_keys # 设置权限
    ```
    在 `master` 机器使用 `ssh slave1` 和 `ssh slave2` 命令测试能否免密登录另外两台机器。


### 配置 JDK

1. 在 `master` 机器上，从 [Oracle官网](https://www.oracle.com/java/technologies/downloads/) 下载对应系统的 Java8， **注意**：最新版本为 Java17，但 Java8 对 HBase 支持度最好。

2. 在 `/usr/local/` 目录下新建一个目录 `jdk`，
    ```
    $ mkdir /usr/local/jdk
    $ cd /usr/local/jdk
    ```
    将 Java 安装包上传到 `jdk` 目录下，并解压
    ```
    $ tar -zxvf jdk-8u321-linux-x64.tar.gz
    ```

3. 解压得到新的目录 `jdk1.8.0_321`，进入该目录，并配置环境变量，
    ```
    $ cd jdk1.8.0_321
    $ nano /etc/profile
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
    $ mkdir /usr/local/hadoop
    $ mkdir /usr/local/hadoop/tmp 
    $ mkdir /usr/local/hadoop/var
    $ mkdir /usr/local/hadoop/dfs
    $ mkdir /usr/local/hadoop/dfs/name
    $ mkdir /usr/local/hadoop/dfs/data
    $ cd /usr/local/hadoop
    ```

2. 从 [Apache 镜像站](https://dlcdn.apache.org/hadoop/common/) 下载 Hadoop 稳定发行版，此处选择的 Hadoop 版本为最新的稳定版 `hadoop-$VERSION`，并解压。
    ```
    # 将 $VERSION 更换为相应版本，此处选择的版本是 hadoop-3.2.2，注意此处的链接具有时效性。
    $ wget 'https://dlcdn.apache.org/hadoop/common/hadoop-3.2.2/hadoop-3.2.2.tar.gz'
    $ tar -zxvf hadoop-3.2.2.tar.gz
    $ cd hadoop-3.2.2/
    ```

3. 修改环境配置文件 `etc/hadoop/hadoop-env.sh`，
    ```
    $ nano etc/hadoop/hadoop-env.sh
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
    $ scp -r /usr/local/hadoop/hadoop-3.2.2 root@slave1:/usr/local/hadoop/
    $ scp -r /usr/local/hadoop/hadoop-3.2.2 root@slave2:/usr/local/hadoop/
    ```

6. 启动 Hadoop：
    ```
    # 格式化文件系统，只需要格式化一次，下次启动不需要格式化
    $ ./bin/hdfs namenode -format
    ```
    启动 NameNode 和 DataNode 守护进程：
    ```
    $ ./sbin/start-dfs.sh
    ```
    启动 Yarn：
    ```
    $ ./sbin/start-yarn.sh
    ```
    日志默认保存路径为 `/usr/local/hadoop/hadoop-3.2.2/logs`。现在可以打开 NameNode 的 WebUI，默认为 `http://localhost:9870`。使用以下命令停止进程：
    ```
    $ ./sbin/stop-all.sh
    ```

7. 使用 `jps` 命令查看 Java 进程，此时在 `master` 应该有三个进程 `NameNode`、`SecondaryNameNode`、`ResourceManger`；在 `slave1`、`slave2` 应该有两个进程 `DataNode`、`NodeManager`。
    
---
### ZooKeeper 部署

1. 在 `master` 机器中新建目录 `zookeeper`：
    ```
    $ mkdir /usr/local/zookeeper
    $ cd /usr/local/zookeeper
    ```

2. 在 [Apache 镜像站](https://zookeeper.apache.org/releases.html) 下载最新的稳定版，此处下载的 ZooKeeper 版本为 `Apache ZooKeeper 3.7.0`，**注意**，此处需要下载的是 ZooKeeper 的可执行版本(apache-zookeeper-3.7.0-bin)，而并不是源码。下载完成之后解压：
    ```
    $ wget 'https://dlcdn.apache.org/zookeeper/zookeeper-3.7.0/apache-zookeeper-3.7.0-bin.tar.gz'
    $ tar -xvzf apache-zookeeper-3.7.0-bin.tar.gz
    $ cd apache-zookeeper-3.7.0-bin
    ```

3. 创建 `conf/zoo.cfg` 配置文件，添加以下配置：
    ```
    # tickTime：ZooKeeper 使用的基本时间单位，以毫秒为单位。
    tickTime=2000
    # dataDir：存储内存中数据库快照的位置，以及更新数据库的日志。
    # myid 文件需要保存到该位置
    dataDir=/usr/local/zookeeper/apache-zookeeper-3.7.0-bin/data  
    # clientPort：侦听客户端连接的端口
    clientPort=2181
    # server. 之后的数字为机器的编号 myid
    server.1=master:2888:3888
    server.2=slave1:2888:3888
    server.3=slave2:2888:3888
    ```
    保存退出。

4. 创建 `myid` 文件，写入编号：
    ```
    $ mkdir data
    $ nano data/myid
    # 写入 master 机器编号 1
    1
    ```

5. 将配置好的 ZooKeeper 目录复制到其他两台机器上，并修改对应的 `myid`，`slave1` 对应编号 `2`，`slave2` 对应编号 `3`。
    ```
    $ scp -r /usr/local/zookeeper root@slave1:/usr/local/
    $ scp -r /usr/local/zookeeper root@slave2:/usr/local/
    ```

6. 启动集群，以下命令三台机器都需要运行：
    ```
    $ /usr/local/zookeeper/apache-zookeeper-3.7.0-bin/bin/zkServer.sh start
    ```
    查看集群的状态：
    ```
    $ /usr/local/zookeeper/apache-zookeeper-3.7.0-bin/bin/zkServer.sh status
    ```
    集群的状态输出如下（`Mode` 会有一个 `leader`，两个 `follower`）：
    ```
    ZooKeeper JMX enabled by default
    Using config: /usr/local/zookeeper/apache-zookeeper-3.7.0-bin/bin/../conf/zoo.cfg
    Client port found: 2181. Client address: localhost. Client SSL: false.
    Mode: follower
    ```

7. 连接到 ZooKeeper 客户端：
    ```
    $ ./bin/zkCli.sh -server 127.0.0.1:2181
    ```
    输入 `quit` 退出 ZooKeeper 客户端。
    
---
### HBase 部署

1. 在 `master` 机器新建以下目录：
    ```
    $ mkdir /usr/local/hbase
    $ mkdir /usr/local/hbase/tmp
    $ cd /usr/local/hbase
    ```

2. 在 [Apache 镜像站](https://dlcdn.apache.org/hbase/) 下载 HBase 镜像，此处选择的是最新的稳定版本 `hbase-2.4.11-bin`。下载完成后解压。
    ```
    $ wget 'https://dlcdn.apache.org/hbase/stable/hbase-2.4.11-bin.tar.gz'
    $ tar -zxvf hbase-2.4.11-bin.tar.gz
    $ cd hbase-2.4.11
    ```

3. 确保在启动 HBase 之前，已经设置了 `JAVA_HOME` 环境变量。修改 `conf/HBase-env.sh` 文件，添加以下内容：
    ```
    # Java 的安装路径
    export JAVA_HOME=/usr/local/jdk/jdk1.8.0_321  
    # 不使用 HBase 自带的 ZooKeeper
    export HBASE_MANAGES_ZK=false 
    ```

4. 修改 `conf/hbase-site.xml` 文件，在 `<configuration>` 节点加入以下配置：
    ```
    <property>
        <name>hbase.cluster.distributed</name>
        <value>true</value>
    </property>
    <property>
        <name>hbase.wal.provider</name>
        <value>filesystem</value>
    </property>
    <property>
        <name>hbase.rootdir</name>
        <value>hdfs://master:9000/hbase</value>
    </property>
    <property>
        <name>hbase.zookeeper.quorum</name>
        <value>master,slave1,slave2</value>
    </property>
    <property>
        <name>hbase.zookeeper.property.dataDir</name>
        <value>/usr/local/zookeeper/apache-zookeeper-3.7.0-bin/data</value>
    </property>
    <property>  
        <name>hbase.table.sanity.checks</name>  
        <value>false</value>  
    </property>
    ```

5. 配置 `conf/regionservers`，添加从分布式机器的主机名：
    ```
    slave1
    slave2
    ```

6. 将 HBase 的配置文件复制到其他机器：
    ```
    $ scp -r /usr/local/hbase root@slave1:/usr/local/
    $ scp -r /usr/local/hbase root@slave2:/usr/local/
    ```

7. 启动 HBase：
    ```
    $ ./bin/start-hbase.sh
    ```
    使用 `jps` 来查看，应该会有一个名叫 `HMaster` 的进程，默认的 WebUI 为 `http://master:16010`，在各个 slave 上运行 `jps` 应该会有 `HRegionServer` 进程。

8. 连接到 HBase：
    ```
    $ ./bin/hbase shell
    ```
    使用 `help` 命令来查看 HBase Shell 的一些基本使用信息，使用 `quit` 命令退出 HBase Shell。

9. 使用以下命令停止所有的 HBase 守护进程：
    ```
    $ ./bin/stop-hbase.sh
    ```

---
### GeoMesa-HBase 部署

1.  在 `master` 机器中新建目录 `geomesa`：
    ```
    $ mkdir /usr/local/geomesa
    $ cd /usr/local/geomesa
    ``` 
    从 [GitHub](https://github.com/locationtech/geomesa/releases/) 下载编译完成的二进制文件（bin），此处选的的是 `geomesa-hbase_2.11-3.2.2-bin.tar.gz`，**注意**：geomesa 有两个版本号，前面的 `2.11` 是它支持的 scala 的版本号，后面的才是它本身的版本号。下载完成之后进行解压：
    ```
    $ wget 'https://github.com/locationtech/geomesa/releases/download/geomesa-3.2.2/geomesa-hbase_2.11-3.2.2-bin.tar.gz'
    $ tar -zxvf geomesa-hbase_2.11-3.2.2-bin.tar.gz
    $ cd geomesa-hbase_2.11-3.2.2
    ```

2. 修改配置信息。在路径 `conf/geomesa-env.sh` 中添加以下环境变量：
    ```
    export HADOOP_HOME=/usr/local/hadoop/hadoop-3.2.2
    export HBASE_HOME=/usr/local/hbase/hbase-2.4.11
    export GEOMESA_HBASE_HOME=/usr/local/geomesa/geomesa-hbase_2.11-3.2.2
    export PATH="${PATH}:${GEOMESA_HBASE_HOME}/bin"
    ```
    **注意**：配置 GeoMesa 的环境变量是 `GEOMESA_HBASE_HOME`。

3. 手动执行以下命令，安装所需插件：
    ```
    $ ./bin/install-shapefile-support.sh
    ```

4. 部署 jar 包，需要将 GeoMesa 的 runtime jar 包拷贝到 HBase 安装目录的 `lib` 文件夹，需要将该 jar 包复制到其他分布式机器上。
    ```
    $ cp /usr/local/geomesa/geomesa-hbase_2.11-3.2.2/dist/hbase/geomesa-hbase-distributed-runtime-hbase2_2.11-3.2.2.jar /usr/local/hbase/hbase-2.4.11/lib/ 
    $ scp -r /usr/local/hbase/hbase-2.4.11/lib root@slave1:/usr/local/hbase/hbase-2.4.11/
    $ scp -r /usr/local/hbase/hbase-2.4.11/lib root@slave2:/usr/local/hbase/hbase-2.4.11/
    ```

5. 注册协处理器（Coprocessors）。使 HBase 在运行时能够访问到 `geomesa-hbase-distributed-runtime` 的 jar 包，需要在三台机器的 HBase 配置文件 `hbase-site.xml` 中添加以下内容：
    ```
    <property>
        <name>hbase.coprocessor.user.region.classes</name>
        <value>org.locationtech.geomesa.hbase.server.coprocessor.GeoMesaCoprocessor</value>
    </property>
    ```

6. 设置命令行工具，将 HBase 配置文件 `hbase-site.xml` 打包进 `geomesa-hbase-datastore_2.11-3.2.2.jar` 中：
    ```
    $ zip -r /usr/local/geomesa/geomesa-hbase_2.11-3.2.2/lib/geomesa-hbase-datastore_2.11-3.2.2.jar /usr/local/hbase/hbase-2.4.11/conf/hbase-site.xml
    ```

7. 重新启动 Hadoop、HBase、ZooKeeper，然后查看 geomesa-hbase 版本：
    ```
    # 重新启动 HBase、Hadoop、ZooKeeper，注意关闭启动的顺序
    $ /usr/local/hbase/hbase-2.4.9/bin/stop-hbase.sh
    $ /usr/local/hadoop/hadoop-3.3.1/sbin/stop-all.sh 
    $ /usr/local/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkServer.sh stop
    $ /usr/local/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkServer.sh start
    $ /usr/local/hadoop/hadoop-3.3.1/sbin/start-all.sh 
    $ /usr/local/hbase/hbase-2.4.9/bin/start-hbase.sh
    
    # 查看 geomesa 版本，这个过程可能会下载几个 jar 包
    $ /usr/local/geomesa/geomesa-hbase_2.11-3.2.2/bin/geomesa-hbase version
    # 输出以下内容，则说明部署成功
    GeoMesa tools version: 3.2.2
    Commit ID: 37e202eb97f64e612d0fecec75e7cdbfc280e67f
    Branch: 37e202eb97f64e612d0fecec75e7cdbfc280e67f
    Build date: 2021-12-09T14:30:52+0000
    ```

8. 注意：如果要运行官方的 [quickstart](https://github.com/geomesa/geomesa-tutorials)，一定要注意 GeoMesa 和 geomesa-tutorials 版本对应。例如 GeoMesa 的版本为 `3.2.2`，那要选择 `3.2.0` 版本的 geomesa-tutorials。
    ```
    # 如果 geomesa 的操作卡住或无法完成，可以先关闭 HBase，再利用 zookeeper 和 Hadoop 的命令来删除 HBase 中的表
    
    # zookeeper 删除 HBase 表
    $ /usr/local/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkCli.sh
    $ deleteall /hbase
    $ quit

    # Hadoop 删除 HBase 表
    $ /usr/local/hadoop/hadoop-3.2.2/bin/hdfs dfs -rm -r -f /hbase

    # 启动 HBase
    $ /usr/local/hbase/hbase-2.4.9/bin/start-hbase.sh
    ```
