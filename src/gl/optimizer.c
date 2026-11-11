#include "optimizer.h"

#include <stdlib.h>
#include <string.h>
#include "config.h"
#include "init.h"
#include "logs.h"
#include "loader.h"
#include "gl4es.h"
#include "../glx/hardext.h"
#include "GLES3/gl32.h"

optimizer_t optimizer;

// Baca environment variable (diatur dari menu renderer plugin di launcher).
// Mengembalikan -1 kalau tidak diset.
static int env_int(const char* name) {
    const char* v = getenv(name);
    if (!v || !*v) return -1;
    return atoi(v);
}

// Urutan prioritas: environment variable, lalu config.json, lalu nilai default.
static int cfg_flag(const char* env_name, char* json_name, int def) {
    int v = env_int(env_name);
    if (v < 0) v = config_get_int(json_name);
    return (v < 0) ? def : (v != 0);
}

// Deteksi PowerVR. Lewat driver native, GL_VENDOR berisi "Imagination Technologies"
// (sudah dideteksi di hardext). Lewat ANGLE, GL_VENDOR jadi "Google", jadi
// GL_RENDERER ikut dicek.
static int detect_powervr(void) {
    if (hardext.vendor & VEND_IMGTEC) return 1;
    LOAD_GLES(glGetString);
    if (!gles_glGetString) return 0;
    const char* r = (const char*)gles_glGetString(GL_RENDERER);
    return r && (strstr(r, "PowerVR") || strstr(r, "Imagination"));
}

// Opsi (environment variable menang atas config.json di /sdcard/NGG/config.json):
//   NGG_OPTIMIZER / "optimizer": tidak ada atau -1 = otomatis (aktif hanya di PowerVR)
//                                0 = mati, 1 = paksa aktif
//   NGG_OPT_FINISH_TO_FLUSH / "opt_finish_to_flush": 0/1, default 1 saat aktif
//   NGG_OPT_SKIP_GETERROR   / "opt_skip_geterror":   0/1, default 1 saat aktif
void optimizer_init(void) {
    memset(&optimizer, 0, sizeof(optimizer));

    int mode = env_int("NGG_OPTIMIZER");
    if (mode < 0) mode = config_get_int("optimizer");
    int is_pvr = detect_powervr();
    optimizer.active = (mode < 0) ? is_pvr : (mode != 0);

    if (!optimizer.active) {
        SHUT_LOGD("Optimizer: off (powervr=%d, config=%d)", is_pvr, mode);
        return;
    }

    optimizer.finish_to_flush = cfg_flag("NGG_OPT_FINISH_TO_FLUSH", "opt_finish_to_flush", 1);
    optimizer.skip_geterror = cfg_flag("NGG_OPT_SKIP_GETERROR", "opt_skip_geterror", 1);

    // glGetError() sering dipanggil tiap frame; langsung kembalikan GL_NO_ERROR
    // memakai mekanisme yang sudah ada (LIBGL_NOERROR).
    if (optimizer.skip_geterror) globals4es.noerror = 1;

    SHUT_LOGD("Optimizer: on (powervr=%d) finish_to_flush=%d skip_geterror=%d", is_pvr, optimizer.finish_to_flush,
              optimizer.skip_geterror);
}
