// 用 mineflayer 当"真玩家"：连上服务器 -> 打印收到的所有聊天（含 tellraw）。
// MF_PATH 可用 npm 装的 mineflayer 路径（默认按 node_modules 解析）。
const mineflayer = require(process.env.MF_PATH || 'mineflayer');
const version = process.argv[2], host = process.argv[3] || '127.0.0.1', port = parseInt(process.argv[4] || '25565');
const bot = mineflayer.createBot({ host, port, username: process.env.BOT_NAME || 'ttcheck_test', version, auth: 'offline' });
bot.on('spawn', () => { console.log('BOT_SPAWNED ' + bot.entity.position); });
bot.on('message', (msg) => { console.log('CHAT: ' + msg.toString()); });
bot.on('error', (e) => console.log('BOT_ERR ' + e.message));
bot.on('kicked', (r) => console.log('BOT_KICKED ' + JSON.stringify(r)));
if (process.env.BOT_STOP_MS) setTimeout(() => { bot.quit(); process.exit(0); }, parseInt(process.env.BOT_STOP_MS));
