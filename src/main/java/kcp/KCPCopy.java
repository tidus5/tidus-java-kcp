package kcp;

import java.util.ArrayList;

public abstract class KCPCopy {


    //=====================================================================
    // KCP BASIC
    //=====================================================================
    public final int IKCP_RTO_NDL = 30;   // no delay min rto
    public final int IKCP_RTO_MIN = 100;  // normal min rto
    public final int IKCP_RTO_DEF = 200;
    public final int IKCP_RTO_MAX = 60000;
    public final int IKCP_CMD_PUSH = 81;  // cmd: push data
    public final int IKCP_CMD_ACK = 82;   // cmd: ack
    public final int IKCP_CMD_WASK = 83;  // cmd: window probe (ask)
    public final int IKCP_CMD_WINS = 84;  // cmd: window size (tell)
    public final int IKCP_ASK_SEND = 1;   // need to send IKCP_CMD_WASK
    public final int IKCP_ASK_TELL = 2;   // need to send IKCP_CMD_WINS
    public final int IKCP_WND_SND = 32;
    public final int IKCP_WND_RCV = 32;
    public final int IKCP_MTU_DEF = 1400;
    public final int IKCP_ACK_FAST = 3;
    public final int IKCP_INTERVAL = 100;
    public final int IKCP_OVERHEAD = 24;
    public final int IKCP_DEADLINK = 10;
    public final int IKCP_THRESH_INIT = 2;
    public final int IKCP_THRESH_MIN = 2;
    public final int IKCP_PROBE_INIT = 7000;    // 7 secs to probe window size
    public final int IKCP_PROBE_LIMIT = 120000; // up to 120 secs to probe window

    protected abstract void output(byte[] buffer, int size); // 需具体实现

    // encode 8 bits unsigned int
    public static void ikcp_encode8u(byte[] p, int offset, byte c) {
        p[0 + offset] = c;
    }

    // decode 8 bits unsigned int
    public static byte ikcp_decode8u(byte[] p, int offset) {
        return p[0 + offset];
    }

    /* encode 16 bits unsigned int (msb) */
    public static void ikcp_encode16u(byte[] p, int offset, int w) {
        p[offset + 0] = (byte) (w >> 8);
        p[offset + 1] = (byte) (w >> 0);
    }

    /* decode 16 bits unsigned int (msb) */
    public static int ikcp_decode16u(byte[] p, int offset) {
        int ret = (p[offset + 0] & 0xFF) << 8
                | (p[offset + 1] & 0xFF);
        return ret;
    }

    /* encode 32 bits unsigned int (msb) */
    public static void ikcp_encode32u(byte[] p, int offset, long l) {
        p[offset + 0] = (byte) (l >> 24);
        p[offset + 1] = (byte) (l >> 16);
        p[offset + 2] = (byte) (l >> 8);
        p[offset + 3] = (byte) (l >> 0);
    }

    /* decode 32 bits unsigned int (msb) */
    public static long ikcp_decode32u(byte[] p, int offset) {
        long ret = (p[offset + 0] & 0xFFL) << 24
                | (p[offset + 1] & 0xFFL) << 16
                | (p[offset + 2] & 0xFFL) << 8
                | p[offset + 3] & 0xFFL;
        return ret;
    }

    /**
     * 只保留 start 到 stop 的几个元素
     */
    public static void slice(ArrayList list, int start, int stop) {
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            if (i < stop - start) {
                list.set(i, list.get(i + start));
            } else {
                list.remove(stop - start);
            }
        }
    }

    static long _imin_(long a, long b) {
        return a <= b ? a : b;
    }

    static long _imax_(long a, long b) {
        return a >= b ? a : b;
    }

    static long _ibound_(long lower, long middle, long upper) {
        return _imin_(_imax_(lower, middle), upper);
    }

    static int _itimediff(long later, long earlier) {
        return ((int) (later - earlier));
    }

    private class Segment {

        protected long conv = 0;
        protected long cmd = 0;
        protected long frg = 0;
        protected long wnd = 0;
        protected long ts = 0;
        protected long sn = 0;
        protected long una = 0;
        protected long resendts = 0;
        protected long rto = 0;
        protected long fastack = 0;
        protected long xmit = 0;
        protected byte[] data;

        protected Segment(int size) {
            this.data = new byte[size];
        }

        //---------------------------------------------------------------------
        // ikcp_encode_seg
        //---------------------------------------------------------------------
        // encode a segment into buffer
        protected int encode(byte[] ptr, int offset) {
            int offset_ = offset;

            ikcp_encode32u(ptr, offset, conv);
            offset += 4;
            ikcp_encode8u(ptr, offset, (byte) cmd);
            offset += 1;
            ikcp_encode8u(ptr, offset, (byte) frg);
            offset += 1;
            ikcp_encode16u(ptr, offset, (int) wnd);
            offset += 2;
            ikcp_encode32u(ptr, offset, ts);
            offset += 4;
            ikcp_encode32u(ptr, offset, sn);
            offset += 4;
            ikcp_encode32u(ptr, offset, una);
            offset += 4;
            ikcp_encode32u(ptr, offset, (long) data.length);
            offset += 4;

            return offset - offset_;
        }
    }


    long conv = 0;
    //long user = user;
    long snd_una = 0;
    long snd_nxt = 0;
    long rcv_nxt = 0;
    long ts_recent = 0;
    long ts_lastack = 0;
    long ts_probe = 0;
    long probe_wait = 0;
    long snd_wnd = IKCP_WND_SND;
    long rcv_wnd = IKCP_WND_RCV;
    long rmt_wnd = IKCP_WND_RCV;
    long cwnd = 0;
    long incr = 0;
    long probe = 0;
    long mtu = IKCP_MTU_DEF;
    long mss = this.mtu - IKCP_OVERHEAD;
    byte[] buffer = new byte[(int) (mtu + IKCP_OVERHEAD) * 3];
    ArrayList<Segment> nrcv_buf = new ArrayList<>(128);
    ArrayList<Segment> nsnd_buf = new ArrayList<>(128);
    ArrayList<Segment> nrcv_que = new ArrayList<>(128);
    ArrayList<Segment> nsnd_que = new ArrayList<>(128);
    long state = 0;
    ArrayList<Long> acklist = new ArrayList<>(128);
    //long ackblock = 0;
    //long ackcount = 0;
    long rx_srtt = 0;
    long rx_rttval = 0;
    long rx_rto = IKCP_RTO_DEF;
    long rx_minrto = IKCP_RTO_MIN;
    long current = 0;
    long interval = IKCP_INTERVAL;
    long ts_flush = IKCP_INTERVAL;
    long nodelay = 0;
    long updated = 0;
    long logmask = 0;
    long ssthresh = IKCP_THRESH_INIT;
    long fastresend = 0;
    long nocwnd = 0;
    long xmit = 0;
    long dead_link = IKCP_DEADLINK;
    //long output = NULL;
    //long writelog = NULL;

    public KCPCopy(long conv_) {
        conv = conv_;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////

    //---------------------------------------------------------------------
    // user/upper level recv: returns size, returns below zero for EAGAIN
    //---------------------------------------------------------------------
    // 将接收队列中的数据传递给上层引用
    public int Recv(byte[] buffer) {//viewing

        if (0 == nrcv_que.size()) {
            return -1;
        }

        int peekSize = PeekSize();
        if (0 > peekSize) {
            return -2;
        }

        if (peekSize > buffer.length) {
            return -3;
        }

        boolean recover = false;
        if (nrcv_que.size() >= rcv_wnd) {
            recover = true;
        }

        // merge fragment.
        int count = 0;
        int n = 0;
        for (Segment seg : nrcv_que) {
            System.arraycopy(seg.data, 0, buffer, n, seg.data.length);
            n += seg.data.length;
            count++;

//            if (ikcp_canlog(kcp, IKCP_LOG_RECV)) {
//                ikcp_log(kcp, IKCP_LOG_RECV, "recv sn=%lu", seg->sn);
//            }
            if (0 == seg.frg) {
                break;
            }
        }
        assert(n == peekSize);

        boolean ispeek = buffer.length < 0;
        if(ispeek == false) {
            //原版kcp这里判断，  是false 才删除
        }
        if (0 < count) {
            slice(nrcv_que, count, nrcv_que.size());
        }



        // move available data from rcv_buf -> nrcv_que
        count = 0;
        for (Segment seg : nrcv_buf) {
            if (seg.sn == rcv_nxt && nrcv_que.size() < rcv_wnd) {
                nrcv_que.add(seg);
                rcv_nxt++;
                count++;
            } else {
                break;
            }
        }

        if (0 < count) {
            slice(nrcv_buf, count, nrcv_buf.size());
        }

        // fast recover
        if (nrcv_que.size() < rcv_wnd && recover) {
            // ready to send back IKCP_CMD_WINS in ikcp_flush
            // tell remote my window size
            probe |= IKCP_ASK_TELL;
        }

        return n;
    }

    //---------------------------------------------------------------------
    // peek data size
    //---------------------------------------------------------------------
    // check the size of next message in the recv queue
    // 计算接收队列中有多少可用的数据
    public int PeekSize() {//viewing
        if (0 == nrcv_que.size()) {
            return -1;
        }

        Segment seq = nrcv_que.get(0);

        if (0 == seq.frg) {
            return seq.data.length;
        }

        if (nrcv_que.size() < seq.frg + 1) {
            return -1;
        }

        int length = 0;

        for (Segment item : nrcv_que) {
            length += item.data.length;
            if (0 == item.frg) {
                break;
            }
        }

        return length;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////








    // 接收窗口可用大小
    int wnd_unused() {
        if (nrcv_que.size() < rcv_wnd) {
            return (int) (int) rcv_wnd - nrcv_que.size();
        }
        return 0;
    }

    void flush() {//viewed
        long current_ = current;
        byte[] buffer_ = buffer;
        int change = 0;
        int lost = 0;

        // 'ikcp_update' haven't been called.
        if (0 == updated) {
            return;
        }

        Segment seg = new Segment(0);
        seg.conv = conv;
        seg.cmd = IKCP_CMD_ACK;
        seg.wnd = (long) wnd_unused();
        seg.una = rcv_nxt;

        // flush acknowledges
        // 将acklist中的ack发送出去
        int count = acklist.size() / 2;
        int offset = 0;
        for (int i = 0; i < count; i++) {
            if (offset + IKCP_OVERHEAD > mtu) {
                output(buffer, offset);
                offset = 0;
            }
            // ikcp_ack_get
            seg.sn = acklist.get(i * 2 + 0);
            seg.ts = acklist.get(i * 2 + 1);
            offset += seg.encode(buffer, offset);
        }
        acklist.clear();

        // probe window size (if remote window size equals zero)
        // rmt_wnd=0时，判断是否需要请求对端接收窗口
        if (0 == rmt_wnd) {
            if (0 == probe_wait) {
                probe_wait = IKCP_PROBE_INIT;
                ts_probe = current + probe_wait;
            } else {
                // 逐步扩大请求时间间隔
                if (_itimediff(current, ts_probe) >= 0) {
                    if (probe_wait < IKCP_PROBE_INIT) {
                        probe_wait = IKCP_PROBE_INIT;
                    }
                    probe_wait += probe_wait / 2;
                    if (probe_wait > IKCP_PROBE_LIMIT) {
                        probe_wait = IKCP_PROBE_LIMIT;
                    }
                    ts_probe = current + probe_wait;
                    probe |= IKCP_ASK_SEND;
                }
            }
        } else {
            ts_probe = 0;
            probe_wait = 0;
        }

        // flush window probing commands
        // 请求对端接收窗口
        if ((probe & IKCP_ASK_SEND) != 0) {
            seg.cmd = IKCP_CMD_WASK;
            if (offset + IKCP_OVERHEAD > mtu) {
                output(buffer, offset);
                offset = 0;
            }
            offset += seg.encode(buffer, offset);
        }

        // flush window probing commands(c#)
        // 告诉对端自己的接收窗口
        if ((probe & IKCP_ASK_TELL) != 0) {
            seg.cmd = IKCP_CMD_WINS;
            if (offset + IKCP_OVERHEAD > mtu) {
                output(buffer, offset);
                offset = 0;
            }
            offset += seg.encode(buffer, offset);
        }

        probe = 0;

        // calculate window size
        long cwnd_ = _imin_(snd_wnd, rmt_wnd);
        // 如果采用拥塞控制
        if (0 == nocwnd) {
            cwnd_ = _imin_(cwnd, cwnd_);
        }

        count = 0;
        // move data from snd_queue to snd_buf
        for (Segment nsnd_que1 : nsnd_que) {
            if (_itimediff(snd_nxt, snd_una + cwnd_) >= 0) {
                break;
            }
            Segment newseg = nsnd_que1;
            newseg.conv = conv;
            newseg.cmd = IKCP_CMD_PUSH;
            newseg.wnd = seg.wnd;
            newseg.ts = current_;
            newseg.sn = snd_nxt;
            newseg.una = rcv_nxt;
            newseg.resendts = current_;
            newseg.rto = rx_rto;
            newseg.fastack = 0;
            newseg.xmit = 0;
            nsnd_buf.add(newseg);
            snd_nxt++;
            count++;
        }

        if (0 < count) {
            slice(nsnd_que, count, nsnd_que.size());
        }

        // calculate resent
        long resent = (fastresend > 0) ? fastresend : 0xffffffff;
        long rtomin = (nodelay == 0) ? (rx_rto >> 3) : 0;

        // flush data segments
        for (Segment segment : nsnd_buf) {
            boolean needsend = false;
            if (0 == segment.xmit) {
                // 第一次传输
                needsend = true;
                segment.xmit++;
                segment.rto = rx_rto;
                segment.resendts = current_ + segment.rto + rtomin;
            } else if (_itimediff(current_, segment.resendts) >= 0) {
                // 丢包重传
                needsend = true;
                segment.xmit++;
                xmit++;
                if (0 == nodelay) {
                    segment.rto += rx_rto;
                } else {
                    segment.rto += rx_rto / 2;
                }
                segment.resendts = current_ + segment.rto;
                lost = 1;
            } else if (segment.fastack >= resent) {
                // 快速重传
                needsend = true;
                segment.xmit++;
                segment.fastack = 0;
                segment.resendts = current_ + segment.rto;
                change++;
            }

            if (needsend) {
                segment.ts = current_;
                segment.wnd = seg.wnd;
                segment.una = rcv_nxt;

                int need = IKCP_OVERHEAD + segment.data.length;
                if (offset + need >= mtu) {
                    output(buffer, offset);
                    offset = 0;
                }

                offset += segment.encode(buffer, offset);
                if (segment.data.length > 0) {
                    System.arraycopy(segment.data, 0, buffer, offset, segment.data.length);
                    offset += segment.data.length;
                }

                if (segment.xmit >= dead_link) {
                    state = -1; // state = 0(c#)
                }
            }
        }

        // flash remain segments
        if (offset > 0) {
            output(buffer, offset);
        }

        // update ssthresh
        // 拥塞避免
        if (change != 0) {
            long inflight = snd_nxt - snd_una;
            ssthresh = inflight / 2;
            if (ssthresh < IKCP_THRESH_MIN) {
                ssthresh = IKCP_THRESH_MIN;
            }
            cwnd = ssthresh + resent;
            incr = cwnd * mss;
        }

        if (lost != 0) {
            ssthresh = cwnd / 2;
            if (ssthresh < IKCP_THRESH_MIN) {
                ssthresh = IKCP_THRESH_MIN;
            }
            cwnd = 1;
            incr = mss;
        }

        if (cwnd < 1) {
            cwnd = 1;
            incr = mss;
        }
    }

    //---------------------------------------------------------------------
    // update state (call it repeatedly, every 10ms-100ms), or you can ask
    // ikcp_check when to call it again (without ikcp_input/_send calling).
    // 'current' - current timestamp in millisec.
    //---------------------------------------------------------------------
    public void Update(long current_) {//viewed

        current = current_;

        // 首次调用Update
        if (0 == updated) {
            updated = 1;
            ts_flush = current;
        }

        // 两次更新间隔
        int slap = _itimediff(current, ts_flush);

        // interval设置过大或者Update调用间隔太久
        if (slap >= 10000 || slap < -10000) {
            ts_flush = current;
            slap = 0;
        }

        // flush同时设置下一次更新时间
        if (slap >= 0) {
            ts_flush += interval;
            if (_itimediff(current, ts_flush) >= 0) {
                ts_flush = current + interval;
            }
            flush();
        }
    }
}
