// Atalhos
const $ = selector => document.querySelector(selector);
const ael = (elem, ev, cb) => elem.addEventListener(ev, cb);

// Vibração: Android quando estiver no APK,
// navegador quando estiver no GitHub Pages.
const vibrate = ms => {
    if (typeof Android !== "undefined" && Android.Vibrate) {
        Android.Vibrate(ms);
    } else if (navigator.vibrate) {
        navigator.vibrate(ms);
    }
};

// Elementos da página
const textareaEncode = $("#encode");
const textareaDecode = $("#decode");
const buttonTextareaEncode = $("#bEncode");
const buttonTextareaDecode = $("#bDecode");

// Codificar
ael(buttonTextareaEncode, "click", () => {
    vibrate(500);
    textareaDecode.value = btoa(textareaEncode.value);
});

// Decodificar
ael(buttonTextareaDecode, "click", () => {
    vibrate(250);
    textareaEncode.value = atob(textareaDecode.value);
});
