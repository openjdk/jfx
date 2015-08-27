
.function audio_convert_orc_unpack_u8
.dest 4 d1 gint32
.source 1 s1 guint8
.param 4 p1
.const 4 c1 0x80000000
.temp 2 t2
.temp 4 t3

convubw t2, s1
convuwl t3, t2
shll t3, t3, p1
xorl d1, t3, c1


.function audio_convert_orc_unpack_s8
.dest 4 d1 gint32
.source 1 s1 guint8
.param 4 p1
.temp 2 t2
.temp 4 t3

convubw t2, s1
convuwl t3, t2
shll d1, t3, p1


.function audio_convert_orc_unpack_u16
.dest 4 d1 gint32
.source 2 s1 guint8
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t2

convuwl t2, s1
shll t2, t2, p1
xorl d1, t2, c1


.function audio_convert_orc_unpack_s16
.dest 4 d1 gint32
.source 2 s1 guint8
.param 4 p1
.temp 4 t2

convuwl t2, s1
shll d1, t2, p1


.function audio_convert_orc_unpack_u16_swap
.dest 4 d1 gint32
.source 2 s1 guint8
.param 4 p1
.const 4 c1 0x80000000
.temp 2 t1
.temp 4 t2

swapw t1, s1
convuwl t2, t1
shll t2, t2, p1
xorl d1, t2, c1


.function audio_convert_orc_unpack_s16_swap
.dest 4 d1 gint32
.source 2 s1 guint8
.param 4 p1
.temp 2 t1
.temp 4 t2

swapw t1, s1
convuwl t2, t1
shll d1, t2, p1


.function audio_convert_orc_unpack_u32
.dest 4 d1 gint32
.source 4 s1 guint8
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1

shll t1, s1, p1
xorl d1, t1, c1


.function audio_convert_orc_unpack_s32
.dest 4 d1 gint32
.source 4 s1 guint8
.param 4 p1

shll d1, s1, p1


.function audio_convert_orc_unpack_u32_swap
.dest 4 d1 gint32
.source 4 s1 guint8
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1

swapl t1, s1
shll t1, t1, p1
xorl d1, t1, c1


.function audio_convert_orc_unpack_s32_swap
.dest 4 d1 gint32
.source 4 s1 guint8
.param 4 p1
.temp 4 t1

swapl t1, s1
shll d1, t1, p1

.function audio_convert_orc_unpack_float_s32
.source 4 s1 gfloat
.dest 4 d1 guint32
.temp 4 t1

loadl t1, s1
# multiply with 2147483647.0
mulf t1, t1, 0x4F000000
# add 0.5 for rounding
addf t1, t1, 0x3F000000
convfl d1, t1

.function audio_convert_orc_unpack_float_s32_swap
.source 4 s1 gfloat
.dest 4 d1 guint32
.temp 4 t1

swapl t1, s1
# multiply with 2147483647.0
mulf t1, t1, 0x4F000000
# add 0.5 for rounding
addf t1, t1, 0x3F000000
convfl d1, t1

.function audio_convert_orc_unpack_double_s32
.source 8 s1 gdouble
.dest 4 d1 guint32
.temp 8 t1

loadq t1, s1
# multiply with 2147483647.0
muld t1, t1, 0x41DFFFFFFFC00000L
# add 0.5 for rounding
addd t1, t1, 0x3FE0000000000000L
convdl d1, t1

.function audio_convert_orc_unpack_double_s32_swap
.source 8 s1 gdouble
.dest 4 d1 guint32
.temp 8 t1

swapq t1, s1
# multiply with 2147483647.0
muld t1, t1, 0x41DFFFFFFFC00000L
# add 0.5 for rounding
addd t1, t1, 0x3FE0000000000000L
convdl d1, t1

.function audio_convert_orc_unpack_float_double
.dest 8 d1 gdouble
.source 4 s1 gfloat

convfd d1, s1

.function audio_convert_orc_unpack_float_double_swap
.dest 8 d1 gdouble
.source 4 s1 gfloat
.temp 4 t1

swapl t1, s1
convfd d1, t1

.function audio_convert_orc_unpack_double_double
.dest 8 d1 gdouble
.source 8 s1 gdouble

copyq d1, s1

.function audio_convert_orc_unpack_double_double_swap
.dest 8 d1 gdouble
.source 8 s1 gdouble

swapq d1, s1

.function audio_convert_orc_unpack_u8_double
.dest 8 d1 gdouble
.source 1 s1 guint8
.param 4 p1
.const 4 c1 0x80000000
.temp 2 t2
.temp 4 t3

convubw t2, s1
convuwl t3, t2
shll t3, t3, p1
xorl t3, t3, c1
convld d1, t3

.function audio_convert_orc_unpack_s8_double
.dest 8 d1 gdouble
.source 1 s1 guint8
.param 4 p1
.temp 2 t2
.temp 4 t3

convubw t2, s1
convuwl t3, t2
shll t3, t3, p1
convld d1, t3

.function audio_convert_orc_unpack_u16_double
.dest 8 d1 gdouble
.source 2 s1 guint8
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t2

convuwl t2, s1
shll t2, t2, p1
xorl t2, t2, c1
convld d1, t2

.function audio_convert_orc_unpack_s16_double
.dest 8 d1 gdouble
.source 2 s1 guint8
.param 4 p1
.temp 4 t2

convuwl t2, s1
shll t2, t2, p1
convld d1, t2

.function audio_convert_orc_unpack_u16_double_swap
.dest 8 d1 gdouble
.source 2 s1 guint8
.param 4 p1
.const 4 c1 0x80000000
.temp 2 t1
.temp 4 t2

swapw t1, s1
convuwl t2, t1
shll t2, t2, p1
xorl t2, t2, c1
convld d1, t2

.function audio_convert_orc_unpack_s16_double_swap
.dest 8 d1 gdouble
.source 2 s1 guint8
.param 4 p1
.temp 2 t1
.temp 4 t2

swapw t1, s1
convuwl t2, t1
shll t2, t2, p1
convld d1, t2

.function audio_convert_orc_unpack_u32_double
.dest 8 d1 gdouble
.source 4 s1 guint8
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1

shll t1, s1, p1
xorl t1, t1, c1
convld d1, t1

.function audio_convert_orc_unpack_s32_double
.dest 8 d1 gdouble
.source 4 s1 guint8
.param 4 p1
.temp 4 t1

shll t1, s1, p1
convld d1, t1

.function audio_convert_orc_unpack_u32_double_swap
.dest 8 d1 gdouble
.source 4 s1 guint8
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1

swapl t1, s1
shll t1, t1, p1
xorl t1, t1, c1
convld d1, t1

.function audio_convert_orc_unpack_s32_double_swap
.dest 8 d1 gdouble
.source 4 s1 guint8
.param 4 p1
.temp 4 t1

swapl t1, s1
shll t1, t1, p1
convld d1, t1

.function audio_convert_orc_pack_u8
.dest 1 d1 guint8
.source 4 s1 gint32
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1
.temp 2 t2

xorl t1, s1, c1
shrul t1, t1, p1
convlw t2, t1
convwb d1, t2


.function audio_convert_orc_pack_s8
.dest 1 d1 guint8
.source 4 s1 gint32
.param 4 p1
.temp 4 t1
.temp 2 t2

shrsl t1, s1, p1
convlw t2, t1
convwb d1, t2



.function audio_convert_orc_pack_u16
.dest 2 d1 guint8
.source 4 s1 gint32
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1

xorl t1, s1, c1
shrul t1, t1, p1
convlw d1, t1


.function audio_convert_orc_pack_s16
.dest 2 d1 guint8
.source 4 s1 gint32
.param 4 p1
.temp 4 t1

shrsl t1, s1, p1
convlw d1, t1


.function audio_convert_orc_pack_u16_swap
.dest 2 d1 guint8
.source 4 s1 gint32
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1
.temp 2 t2

xorl t1, s1, c1
shrul t1, t1, p1
convlw t2, t1
swapw d1, t2


.function audio_convert_orc_pack_s16_swap
.dest 2 d1 guint8
.source 4 s1 gint32
.param 4 p1
.temp 4 t1
.temp 2 t2

shrsl t1, s1, p1
convlw t2, t1
swapw d1, t2



.function audio_convert_orc_pack_u32
.dest 4 d1 guint8
.source 4 s1 gint32
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1

xorl t1, s1, c1
shrul d1, t1, p1


.function audio_convert_orc_pack_s32
.dest 4 d1 guint8
.source 4 s1 gint32
.param 4 p1

shrsl d1, s1, p1


.function audio_convert_orc_pack_u32_swap
.dest 4 d1 guint8
.source 4 s1 gint32
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1

xorl t1, s1, c1
shrul t1, t1, p1
swapl d1, t1


.function audio_convert_orc_pack_s32_swap
.dest 4 d1 guint8
.source 4 s1 gint32
.param 4 p1
.temp 4 t1

shrsl t1, s1, p1
swapl d1, t1

.function audio_convert_orc_pack_s32_float
.dest 4 d1 gfloat
.source 4 s1 gint32
.temp 4 t1

convlf t1, s1
# divide by 2147483647.0
divf t1, t1, 0x4F000000
storel d1, t1

.function audio_convert_orc_pack_s32_float_swap
.dest 4 d1 gfloat
.source 4 s1 gint32
.temp 4 t1

convlf t1, s1
# divide by 2147483647.0
divf t1, t1, 0x4F000000
swapl d1, t1

.function audio_convert_orc_pack_s32_double
.dest 8 d1 gdouble
.source 4 s1 gint32
.temp 8 t1

convld t1, s1
# divide by 2147483647.0
divd t1, t1, 0x41DFFFFFFFC00000L
storeq d1, t1

.function audio_convert_orc_pack_s32_double_swap
.dest 8 d1 gdouble
.source 4 s1 gint32
.temp 8 t1

convld t1, s1
# divide by 2147483647.0
divd t1, t1, 0x41DFFFFFFFC00000L
swapq d1, t1

.function audio_convert_orc_pack_double_float
.dest 4 d1 gfloat
.source 8 s1 gdouble

convdf d1, s1

.function audio_convert_orc_pack_double_float_swap
.dest 4 d1 gfloat
.source 8 s1 gdouble
.temp 4 t1

convdf t1, s1
swapl d1, t1

.function audio_convert_orc_pack_double_u8
.dest 1 d1 guint8
.source 8 s1 gdouble
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1
.temp 2 t2

convdl t1, s1
xorl t1, t1, c1
shrul t1, t1, p1
convlw t2, t1
convwb d1, t2

.function audio_convert_orc_pack_double_s8
.dest 1 d1 guint8
.source 8 s1 gdouble
.param 4 p1
.temp 4 t1
.temp 2 t2

convdl t1, s1
shrsl t1, t1, p1
convlw t2, t1
convwb d1, t2

.function audio_convert_orc_pack_double_u16
.dest 2 d1 guint8
.source 8 s1 gdouble
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1

convdl t1, s1
xorl t1, t1, c1
shrul t1, t1, p1
convlw d1, t1

.function audio_convert_orc_pack_double_s16
.dest 2 d1 guint8
.source 8 s1 gdouble
.param 4 p1
.temp 4 t1

convdl t1, s1
shrsl t1, t1, p1
convlw d1, t1

.function audio_convert_orc_pack_double_u16_swap
.dest 2 d1 guint8
.source 8 s1 gdouble
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1
.temp 2 t2

convdl t1, s1
xorl t1, t1, c1
shrul t1, t1, p1
convlw t2, t1
swapw d1, t2

.function audio_convert_orc_pack_double_s16_swap
.dest 2 d1 guint8
.source 8 s1 gdouble
.param 4 p1
.temp 4 t1
.temp 2 t2

convdl t1, s1
shrsl t1, t1, p1
convlw t2, t1
swapw d1, t2

.function audio_convert_orc_pack_double_u32
.dest 4 d1 guint8
.source 8 s1 gdouble
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1

convdl t1, s1
xorl t1, t1, c1
shrul d1, t1, p1

.function audio_convert_orc_pack_double_s32
.dest 4 d1 guint8
.source 8 s1 gdouble
.param 4 p1
.temp 4 t1

convdl t1, s1
shrsl d1, t1, p1

.function audio_convert_orc_pack_double_u32_swap
.dest 4 d1 guint8
.source 8 s1 gdouble
.param 4 p1
.const 4 c1 0x80000000
.temp 4 t1

convdl t1, s1
xorl t1, t1, c1
shrul t1, t1, p1
swapl d1, t1

.function audio_convert_orc_pack_double_s32_swap
.dest 4 d1 guint8
.source 8 s1 gdouble
.param 4 p1
.temp 4 t1

convdl t1, s1
shrsl t1, t1, p1
swapl d1, t1

