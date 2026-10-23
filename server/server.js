const { Server } = require('ssh2');
const crypto = require('crypto');
const { spawn } = require('child_process');
const os = require('os');
const fs = require('fs');
const path = require('path');

// 1. Generate or load host private key
const keyPath = path.join(__dirname, 'host_key.pem');
let hostKey;
if (fs.existsSync(keyPath)) {
    hostKey = fs.readFileSync(keyPath, 'utf8');
} else {
    console.log('[Optimus SSH Server] Generating 2048-bit RSA Host Key...');
    const { privateKey } = crypto.generateKeyPairSync('rsa', {
        modulusLength: 2048,
        publicKeyEncoding: { type: 'spki', format: 'pem' },
        privateKeyEncoding: { type: 'pkcs1', format: 'pem' }
    });
    hostKey = privateKey;
    fs.writeFileSync(keyPath, hostKey);
}

// 2. Discover local IP addresses
const networkInterfaces = os.networkInterfaces();
const localIps = [];
for (const iface of Object.values(networkInterfaces)) {
    for (const config of iface) {
        if (config.family === 'IPv4' && !config.internal) {
            localIps.push(config.address);
        }
    }
}

const PORT = 2222;

// 3. Create SSH Server
const server = new Server({
    hostKeys: [hostKey]
}, (client) => {
    console.log(`[+] New incoming SSH connection from ${client._sock.remoteAddress}:${client._sock.remotePort}`);

    client.on('authentication', (ctx) => {
        console.log(`[Auth] Attempted method: ${ctx.method}, username: '${ctx.username}'`);
        
        // Accept password 'optimus' (or any password for user 'optimus')
        if (ctx.method === 'password') {
            console.log(`[Auth] User '${ctx.username}' authenticated successfully.`);
            return ctx.accept();
        }

        // Accept public key authentication
        if (ctx.method === 'publickey') {
            console.log(`[Auth] User '${ctx.username}' authenticated with public key.`);
            return ctx.accept();
        }

        ctx.reject(['password', 'publickey']);
    });

    client.on('ready', () => {
        console.log('[+] Client session ready.');

        client.on('session', (accept, reject) => {
            const session = accept();

            session.once('pty', (accept, reject, info) => {
                console.log(`[PTY] Requested: ${info.term} (${info.cols}x${info.rows})`);
                accept();
            });

            session.on('window-change', (accept, reject, info) => {
                console.log(`[Resize] New dimensions: ${info.cols}x${info.rows}`);
                if (accept) accept();
            });

            session.once('shell', (accept, reject) => {
                const stream = accept();
                console.log('[Shell] Starting interactive shell...');

                // Send welcome banner
                stream.write('\r\n\x1b[1;36m====================================================\x1b[0m\r\n');
                stream.write('\x1b[1;32m   Welcome to Optimus SSH Server on Windows / WSL!  \x1b[0m\r\n');
                stream.write('\x1b[1;34m   Connected from: ' + client._sock.remoteAddress + '\x1b[0m\r\n');
                stream.write('\x1b[1;36m====================================================\x1b[0m\r\n\r\n');

                // Launch cmd.exe or powershell.exe with raw stdio
                const shellProcess = spawn('powershell.exe', ['-NoLogo'], {
                    env: process.env,
                    cwd: os.homedir()
                });

                // Pipe data between SSH channel and child process
                shellProcess.stdout.on('data', (data) => {
                    stream.write(data);
                });

                shellProcess.stderr.on('data', (data) => {
                    stream.write(data);
                });

                stream.on('data', (data) => {
                    shellProcess.stdin.write(data);
                });

                stream.on('close', () => {
                    shellProcess.kill();
                    console.log('[-] Shell stream closed.');
                });

                shellProcess.on('close', (code) => {
                    stream.exit(code || 0);
                    stream.end();
                    console.log(`[-] Shell process exited with code ${code}`);
                });
            });
        });
    });

    client.on('end', () => {
        console.log('[-] Client disconnected.');
    });

    client.on('error', (err) => {
        console.log(`[!] Client error: ${err.message}`);
    });
});

server.listen(PORT, '0.0.0.0', () => {
    console.log('\n================================================================');
    console.log('  OPTIMUS SSH SERVER IS ACTIVE AND LISTENING');
    console.log('================================================================');
    console.log(`  Local IP(s):    ${localIps.join(', ') || '127.0.0.1'}`);
    console.log(`  Port:           ${PORT}`);
    console.log('  Username:       optimus (or your Windows username)');
    console.log('  Password:       optimus (or any password)');
    console.log('================================================================');
    console.log('  In your Optimus Android app, configure:');
    console.log(`    Hostname:     ${localIps[0] || '10.36.99.187'}`);
    console.log(`    Port:         ${PORT}`);
    console.log('    Username:     optimus');
    console.log('    Password:     optimus');
    console.log('================================================================\n');
});
