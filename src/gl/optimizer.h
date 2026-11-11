#ifndef _GL4ES_OPTIMIZER_H_
#define _GL4ES_OPTIMIZER_H_

#ifdef __cplusplus
extern "C" {
#endif

// Optimizer untuk GPU tile-based (PowerVR / Imagination).
// Semua opsi bisa diatur lewat config.json (lihat optimizer.c).
typedef struct {
    int active;          // optimizer aktif atau tidak
    int finish_to_flush; // glFinish() diganti glFlush() (hindari stall pipeline)
    int skip_geterror;  // glGetError() tidak diteruskan ke driver
} optimizer_t;

extern optimizer_t optimizer;

// Dipanggil sekali di initialize_gl4es(), setelah hardext & config terbaca.
void optimizer_init(void);

#ifdef __cplusplus
}
#endif

#endif // _GL4ES_OPTIMIZER_H_
