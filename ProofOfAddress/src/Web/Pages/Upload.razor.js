let url;

export async function setImageUsingStreaming(imageElementId, imageStream) {
    const arrayBuffer = await imageStream.arrayBuffer();
    const blob = new Blob([arrayBuffer]);
    url = URL.createObjectURL(blob);
    document.getElementById(imageElementId).src = url;
}

export async function disposeUrl() {
    if (url) {
        window.URL.revokeObjectURL(url);
    }
}

export async function resetImage(imageElementId) {
    document.getElementById(imageElementId).src = "";
    disposeUrl();
}