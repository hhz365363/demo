// 后台线程：把整个文件读一遍算 SHA-256
// 分片全部上传完之后、合并之前调用，主线程不会因为读大文件而卡住
// 纯 JS 实现（不引第三方库），输出与 Java MessageDigest + HexFormat 一致
const READ_SIZE = 4 * 1024 * 1024

const K = Uint32Array.from([
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
])

// 8 个 32 位中间值，每次算一个文件都要重置成标准初值
const state = new Uint32Array(8)
const W = new Uint32Array(64)
const rest = new Uint8Array(64)
let restLen = 0
let totalLen = 0

/**
 * 重置到 SHA-256 的标准初值，保证不同文件之间不串状态
 */
function reset() {
    state.set([
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    ])
    rest.fill(0)
    restLen = 0
    totalLen = 0
}

const rotr = (x, n) => (x >>> n) | (x << (32 - n))

/**
 * 处理一个 64 字节块
 * @param {Uint8Array} data
 * @param {number} offset
 */
function block(data, offset) {
    for (let i = 0; i < 16; i++) {
        const j = offset + i * 4
        W[i] = (data[j] << 24) | (data[j + 1] << 16) | (data[j + 2] << 8) | data[j + 3]
    }
    for (let i = 16; i < 64; i++) {
        const x = W[i - 15]
        const y = W[i - 2]
        const s0 = rotr(x, 7) ^ rotr(x, 18) ^ (x >>> 3)
        const s1 = rotr(y, 17) ^ rotr(y, 19) ^ (y >>> 10)
        W[i] = (W[i - 16] + s0 + W[i - 7] + s1) | 0
    }
    let a = state[0], b = state[1], c = state[2], d = state[3]
    let e = state[4], f = state[5], g = state[6], h = state[7]
    for (let i = 0; i < 64; i++) {
        const S1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25)
        const ch = (e & f) ^ (~e & g)
        const t1 = (h + S1 + ch + K[i] + W[i]) | 0
        const S0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22)
        const maj = (a & b) ^ (a & c) ^ (b & c)
        const t2 = (S0 + maj) | 0
        h = g
        g = f
        f = e
        e = (d + t1) | 0
        d = c
        c = b
        b = a
        a = (t1 + t2) | 0
    }
    state[0] = (state[0] + a) | 0
    state[1] = (state[1] + b) | 0
    state[2] = (state[2] + c) | 0
    state[3] = (state[3] + d) | 0
    state[4] = (state[4] + e) | 0
    state[5] = (state[5] + f) | 0
    state[6] = (state[6] + g) | 0
    state[7] = (state[7] + h) | 0
}

/**
 * 喂数据：满 64 字节就压一块，不足的留到下次
 * @param {Uint8Array} data
 */
function update(data) {
    totalLen += data.length
    let offset = 0
    if (restLen > 0) {
        const need = 64 - restLen
        if (data.length < need) {
            rest.set(data, restLen)
            restLen += data.length
            return
        }
        rest.set(data.subarray(0, need), restLen)
        block(rest, 0)
        restLen = 0
        offset = need
    }
    while (offset + 64 <= data.length) {
        block(data, offset)
        offset += 64
    }
    if (offset < data.length) {
        rest.set(data.subarray(offset), 0)
        restLen = data.length - offset
    }
}

/**
 * 补位并输出小写十六进制
 * @returns {string}
 */
function digest() {
    const lenBits = BigInt(totalLen) * 8n
    const padLen = restLen < 56 ? 56 - restLen : 120 - restLen
    const last = new Uint8Array(padLen + 8)
    last[0] = 0x80
    for (let i = 0; i < 8; i++) {
        last[padLen + i] = Number((lenBits >> BigInt(56 - i * 8)) & 0xffn)
    }
    update(last)
    let hex = ''
    for (let i = 0; i < 8; i++) {
        hex += (state[i] >>> 0).toString(16).padStart(8, '0')
    }
    return hex
}

self.onmessage = async (e) => {
    const msg = e.data
    if (msg.type !== 'hash-file') {
        return
    }
    const file = msg.file
    reset()
    let loaded = 0
    try {
        while (loaded < file.size) {
            const end = Math.min(loaded + READ_SIZE, file.size)
            const buf = await file.slice(loaded, end).arrayBuffer()
            update(new Uint8Array(buf))
            loaded = end
            self.postMessage({ type: 'progress', loaded: loaded, total: file.size })
        }
        self.postMessage({ type: 'done', hash: digest() })
    } catch (err) {
        self.postMessage({ type: 'error', message: err.message })
    }
}
