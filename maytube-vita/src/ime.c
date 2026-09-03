#include "ime.h"

#include <string.h>

#ifdef __vita__
#include <psp2/ime_dialog.h>
#include <psp2/kernel/threadmgr.h>
#include <psp2/sysmodule.h>

#define IME_MAX_CHARS 256

/* Server URLs are ASCII, so a straight char<->SceWChar16 widen/narrow is
   enough -- no need for a real UTF-8 decoder here. Any non-ASCII byte
   narrows to '?' rather than mangling silently. */
static void utf8_to_utf16(const char *src, SceWChar16 *dst, size_t dst_cap_chars) {
    size_t i = 0;
    for (; src && src[i] && i + 1 < dst_cap_chars; i++) {
        dst[i] = (SceWChar16)(unsigned char)src[i];
    }
    dst[i] = 0;
}

static void utf16_to_utf8(const SceWChar16 *src, char *dst, size_t dst_cap) {
    size_t i = 0;
    for (; src[i] && i + 1 < dst_cap - 1; i++) {
        dst[i] = (src[i] < 0x80) ? (char)src[i] : '?';
    }
    dst[i] = '\0';
}

int ime_prompt_text(const char *title, const char *initial_text, char *out_utf8, size_t out_cap) {
    sceSysmoduleLoadModule(SCE_SYSMODULE_IME);

    static SceWChar16 title16[SCE_IME_DIALOG_MAX_TITLE_LENGTH + 1];
    static SceWChar16 initial16[IME_MAX_CHARS + 1];
    static SceWChar16 buffer16[IME_MAX_CHARS + 1];
    memset(title16, 0, sizeof(title16));
    memset(initial16, 0, sizeof(initial16));
    memset(buffer16, 0, sizeof(buffer16));

    utf8_to_utf16(title, title16, SCE_IME_DIALOG_MAX_TITLE_LENGTH + 1);
    utf8_to_utf16(initial_text, initial16, IME_MAX_CHARS + 1);
    memcpy(buffer16, initial16, sizeof(initial16));

    SceImeDialogParam param;
    sceImeDialogParamInit(&param);
    param.supportedLanguages = 0; /* system default */
    param.languagesForced = SCE_FALSE;
    param.type = 0; /* SCE_IME_TYPE_DEFAULT */
    param.dialogMode = SCE_IME_DIALOG_DIALOG_MODE_WITH_CANCEL;
    param.textBoxMode = SCE_IME_DIALOG_TEXTBOX_MODE_DEFAULT;
    param.title = title16;
    param.maxTextLength = IME_MAX_CHARS;
    param.initialText = initial16;
    param.inputTextBuffer = buffer16;

    if (sceImeDialogInit(&param) < 0) return -1;

    while (sceImeDialogGetStatus() == SCE_COMMON_DIALOG_STATUS_RUNNING) {
        sceKernelDelayThread(16 * 1000);
    }

    SceImeDialogResult result;
    memset(&result, 0, sizeof(result));
    sceImeDialogGetResult(&result);

    int ok = (result.button == SCE_IME_DIALOG_BUTTON_ENTER);
    if (ok) utf16_to_utf8(buffer16, out_utf8, out_cap);

    sceImeDialogTerm();
    return ok ? 0 : -1;
}

#else /* !__vita__ */

int ime_prompt_text(const char *title, const char *initial_text, char *out_utf8, size_t out_cap) {
    (void)title; (void)initial_text; (void)out_utf8; (void)out_cap;
    return -1;
}

#endif
