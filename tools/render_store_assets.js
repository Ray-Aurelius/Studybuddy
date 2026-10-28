// Renders Play Store brand assets from docs/brand/icon.svg using the bundled Marcellus font.
// Usage: node tools/render_store_assets.js <repoRoot>
const { chromium } = require('playwright');
const fs = require('fs'); const path = require('path');
(async () => {
  const root = process.argv[2] || '.';
  const icon = fs.readFileSync(path.join(root, 'docs/brand/icon.svg'), 'utf8');
  const font = 'data:font/ttf;base64,' + fs.readFileSync(path.join(root, 'app/src/main/res/font/marcellus.ttf')).toString('base64');
  const sized = (px) => icon.replace('width="108" height="108"', `width="${px}" height="${px}"`);
  const key = `<svg width="100%" height="18" xmlns="http://www.w3.org/2000/svg"><defs><pattern id="k" width="36" height="18" patternUnits="userSpaceOnUse">
     <path d="M0,16.7 V3 H24 V13 H10 V8 H17 M0,16.7 H36" fill="none" stroke="rgba(255,255,255,.55)" stroke-width="2.6"/></pattern></defs>
     <rect width="100%" height="18" fill="url(#k)"/></svg>`;
  const b = await chromium.launch();
  const p = await b.newPage();
  // 512x512 store icon (Play applies its own mask).
  await p.setViewportSize({ width: 512, height: 512 });
  await p.setContent(`<html><body style="margin:0">${sized(512)}</body></html>`);
  await p.screenshot({ path: path.join(root, 'docs/brand/play-icon-512.png') });
  // 1024x500 feature graphic.
  await p.setViewportSize({ width: 1024, height: 500 });
  await p.setContent(`<html><head><style>
    @font-face { font-family: Marcellus; src: url('${font}'); }
    body { margin:0; width:1024px; height:500px; overflow:hidden; font-family: Marcellus, serif;
      background: linear-gradient(135deg, #2F9BFF 0%, #1E8FE8 45%, #14A8A6 100%); color: #fff; position: relative; }
    .band { position:absolute; left:0; right:0; } .top { top:22px } .bottom { bottom:22px; transform: scaleY(-1) }
    .wrap { position:absolute; inset:0; display:flex; align-items:center; gap:44px; padding: 0 84px; }
    .icon { width:250px; height:250px; border-radius: 56px; overflow:hidden; box-shadow: 0 18px 40px rgba(10,40,80,.28); flex: none }
    h1 { font-size: 92px; margin: 0; font-weight: 400; letter-spacing: 1px; text-shadow: 0 3px 12px rgba(10,40,80,.2) }
    .tag { font-size: 36px; margin-top: 6px; color: #FFE58A }
    .sub { font-family: system-ui, sans-serif; font-size: 22px; margin-top: 18px; opacity: .95; line-height: 1.4 }
  </style></head><body>
    <div class="band top">${key}</div><div class="band bottom">${key}</div>
    <div class="wrap"><div class="icon">${sized(250)}</div>
      <div><h1>Sophinity</h1><div class="tag">Learning never stops.</div>
      <div class="sub">Track your learning time, books, podcasts &amp; courses.<br>Earn laurels. Rise from Neophyte to Philosopher.</div></div></div>
  </body></html>`);
  await p.evaluate(() => document.fonts.ready);
  await p.waitForTimeout(200);
  await p.screenshot({ path: path.join(root, 'docs/brand/play-feature-graphic.png') });
  await b.close();
})();
