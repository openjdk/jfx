# -------------------------------------------------------------------
# Main project file for WebCore
#
# See 'Tools/qmake/README' for an overview of the build system
# -------------------------------------------------------------------

TEMPLATE = subdirs
CONFIG += ordered

SUBDIRS += derived_sources target

derived_sources.file = DerivedSourcesJava.pri
derived_sources.target = generated_files
target.file = TargetJava.pri
target.depend = derived_sources



