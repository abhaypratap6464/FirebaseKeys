cmake_minimum_required(VERSION 3.22.1)
project("firebasekeys")

add_library(keys SHARED keys.cpp)

find_library(log-lib log)
target_link_libraries(keys ${log-lib})
