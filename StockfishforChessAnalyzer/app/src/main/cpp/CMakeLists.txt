# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("stockfishforchessanalyzer")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        stockfishforchessanalyzer

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        #native-lib.cpp
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        stockfishforchessanalyzer

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})

set_target_properties(stockfishforchessanalyzer PROPERTIES LINKER_LANGUAGE CXX)

add_executable(
        stockfish

        benchmark.cpp bitbase.cpp bitboard.cpp bitboard.h endgame.cpp endgame.h evaluate.cpp evaluate.h main.cpp material.cpp material.h misc.cpp misc.h movegen.cpp movegen.h movepick.cpp movepick.h pawns.cpp pawns.h position.cpp position.h psqt.cpp psqt.h search.cpp search.h simd.h thread.cpp thread.h thread_win32_osx.h timeman.cpp timeman.h tt.cpp tt.h tune.cpp tune.h types.h uci.cpp uci.h ucioption.cpp syzygy/tbprobe.cpp syzygy/tbprobe.h nnue/features/half_ka_v2_hm.cpp nnue/features/half_ka_v2_hm.h nnue/layers/affine_transform.h nnue/layers/clipped_relu.h nnue/layers/input_slice.h nnue/evaluate_nnue.cpp nnue/evaluate_nnue.h nnue/nnue_accumulator.h nnue/nnue_architecture.h nnue/nnue_common.h nnue/nnue_feature_transformer.h incbin/incbin.h
)