window.spawnReaction = function(symbol) {
    const el = document.createElement('div');
    el.innerText = symbol;
    el.style.position = 'fixed';
    el.style.bottom = '20px';
    el.style.left = Math.random() * 80 + '%';
    el.style.fontSize = '32px';
    el.style.zIndex = '9999';
    el.style.transition = 'transform 2s ease-out, opacity 2s';
    document.body.appendChild(el);

    requestAnimationFrame(() => {
        el.style.transform = 'translateY(-300px)';
        el.style.opacity = '0';
    });

    setTimeout(() => el.remove(), 2000);
};