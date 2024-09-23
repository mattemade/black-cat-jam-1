
var audioContext

function applySafariAudioHack() {
    audioContext = createAudioContext();
    if (audioContext && audioContext.state !== 'running') {
        registerEventListenersToResume(audioContext);
    }
}

function getOrCreateAudioContext() {
    if (!audioContext) {
        return audioContext;
    }
    var AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (AudioContextClass) {
       var audioContext = new AudioContextClass();
       return audioContext;
    }
    return null;
}

var userInteractionEventNames = [ 'mousedown', 'pointerdown', 'touchstart', 'keydown' ];

function resumeAudioContext(ignoredEventName) {
   audioContext.resume();
   unregisterEventListenersToResume();
}

function registerEventListenersToResume(audioContext) {
    userInteractionEventNames.forEach((eventName) => document.addEventListener(eventName, resumeAudioContext));
}

function unregisterEventListenersToResume() {
     userInteractionEventNames.forEach((eventName) => document.removeEventListener(eventName, resumeAudioContext));
}