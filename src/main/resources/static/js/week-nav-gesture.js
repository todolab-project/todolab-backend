// src/main/resources/static/js/week-nav-gesture.js
(() => {
  const page = document.getElementById("week-page");
  const card = document.getElementById("weekNavCard");
  if (!page || !card) return;

  const prevHref = page.getAttribute("data-prev-href");
  const nextHref = page.getAttribute("data-next-href");

  let locked = false;
  const lock = (ms = 450) => {
    locked = true;
    setTimeout(() => (locked = false), ms);
  };

  const goPrev = () => {
    if (locked || !prevHref) return;
    lock();
    location.href = prevHref;
  };

  const goNext = () => {
    if (locked || !nextHref) return;
    lock();
    location.href = nextHref;
  };

  // 1) 트랙패드/휠 가로 스크롤(shift 포함) → 주 이동
  let accX = 0;
  let wheelTimer = null;

  card.addEventListener(
    "wheel",
    (e) => {
      const dx = Math.abs(e.deltaX) > 0 ? e.deltaX : (e.shiftKey ? e.deltaY : 0);
      if (!dx) return;

      e.preventDefault();

      accX += dx;
      if (wheelTimer) clearTimeout(wheelTimer);

      wheelTimer = setTimeout(() => {
        const threshold = 60;
        if (accX > threshold) goNext();
        else if (accX < -threshold) goPrev();
        accX = 0;
      }, 80);
    },
    { passive: false }
  );

  // 2) 스와이프/드래그 → 주 이동
  let startX = 0;
  let startY = 0;
  let tracking = false;

  card.addEventListener("pointerdown", (e) => {
    tracking = true;
    startX = e.clientX;
    startY = e.clientY;
  });

  card.addEventListener(
    "pointermove",
    (e) => {
      if (!tracking) return;

      const dx = e.clientX - startX;
      const dy = e.clientY - startY;

      if (Math.abs(dy) > Math.abs(dx)) return;

      e.preventDefault();
    },
    { passive: false }
  );

  card.addEventListener("pointerup", (e) => {
    if (!tracking) return;
    tracking = false;

    const dx = e.clientX - startX;
    const dy = e.clientY - startY;

    if (Math.abs(dy) > Math.abs(dx)) return;

    const threshold = 70;
    if (dx <= -threshold) goNext();
    else if (dx >= threshold) goPrev();
  });

  card.addEventListener("pointercancel", () => {
    tracking = false;
  });
})();
