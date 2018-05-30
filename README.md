# tidus-java-kcp

本项目中 KCP.java 来自于https://github.com/hkspirt/kcp-java 

KCPC.java 基于以上项目进行修改
并尽量和原版https://github.com/skywind3000/kcp 接近，方便后续进行更新。

相比hkspirt/kcp-java 做了如下修改
1. 将 ArrayList 改为用LinkedList。 在遍历并移除时开销更小
2. 将编码改为小端，和原版KCP保持一致。（https://github.com/skywind3000/kcp/issues/53）
3. 保留C原版的KCP 中的打印日志语句。
4. 同步原版KCP的部分更新
5. 部分变量名和语句优化



