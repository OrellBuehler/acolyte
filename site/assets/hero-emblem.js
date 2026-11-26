(function () {
  var hx = document.querySelector('.hx');
  if (!hx) return;
  var visible = true;
  function sync() {
    hx.classList.toggle('hx-paused', !visible || document.hidden);
  }
  if ('IntersectionObserver' in window) {
    new IntersectionObserver(function (entries) {
      visible = entries[entries.length - 1].isIntersecting;
      sync();
    }).observe(hx);
  }
  document.addEventListener('visibilitychange', sync);

  var still = window.matchMedia('(prefers-reduced-motion: reduce)');
  var fine = window.matchMedia('(hover: hover) and (pointer: fine)');
  if (still.matches || !fine.matches) return;

  var tx = 0, ty = 0, x = 0, y = 0, raf = 0;
  function frame() {
    raf = 0;
    if (!visible || document.hidden) return;
    x += (tx - x) * 0.08;
    y += (ty - y) * 0.08;
    hx.style.setProperty('--rx', x.toFixed(3));
    hx.style.setProperty('--ry', y.toFixed(3));
    if (Math.abs(tx - x) > 0.002 || Math.abs(ty - y) > 0.002) raf = requestAnimationFrame(frame);
  }
  window.addEventListener('pointermove', function (e) {
    tx = Math.max(-1, Math.min(1, (e.clientX / window.innerWidth) * 2 - 1));
    ty = Math.max(-1, Math.min(1, (e.clientY / window.innerHeight) * 2 - 1));
    if (!raf) raf = requestAnimationFrame(frame);
  }, { passive: true });
}());
