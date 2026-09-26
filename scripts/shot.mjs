// A visual check of the running site: drives a headless Firefox over WebDriver BiDi, opens a URL, waits until a
// JavaScript condition holds (the data fetched, the canvas drawn) and saves a screenshot, which a plain
// `firefox --screenshot` cannot do, since it fires before the fetches complete.
// Start the browser with `firefox --headless --no-remote --profile <dir> --remote-debugging-port <port>`, then
//   node scripts/shot.mjs <port> <url> <condition> <out.png>
import fs from "fs";
const [port, url, cond, out] = process.argv.slice(2);
const ws = new WebSocket(`ws://127.0.0.1:${port}/session`);
let id = 0; const pending = new Map();
const send = (method, params) => new Promise((res, rej) => { const i = ++id; pending.set(i, { res, rej }); ws.send(JSON.stringify({ id: i, method, params })); });
ws.onmessage = m => { const d = JSON.parse(m.data); if (d.id && pending.has(d.id)) { const p = pending.get(d.id); pending.delete(d.id); d.type === "error" ? p.rej(new Error(d.error + ": " + d.message)) : p.res(d.result); } };
await new Promise(r => ws.onopen = r);
await send("session.new", { capabilities: {} });
const { context: ctx } = await send("browsingContext.create", { type: "tab" });
await send("browsingContext.setViewport", { context: ctx, viewport: { width: 1200, height: 900 } });
await send("browsingContext.navigate", { context: ctx, url, wait: "complete" });
let ok = false;
for (let t = 0; t < 100 && !ok; t++) {
  const r = await send("script.evaluate", { expression: `Boolean(${cond})`, target: { context: ctx }, awaitPromise: false });
  ok = r.result && r.result.value === true;
  if (!ok) await new Promise(r => setTimeout(r, 200));
}
await new Promise(r => setTimeout(r, 300));
const shot = await send("browsingContext.captureScreenshot", { context: ctx });
fs.writeFileSync(out, Buffer.from(shot.data, "base64"));
console.log(ok ? "condition met" : "condition NOT met (timeout)", out);
await send("session.end", {}).catch(() => {}); ws.close(); process.exit(0);
