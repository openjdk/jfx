#!/usr/bin/env python3
import os
import subprocess
import sys
from fnmatch import fnmatch
from pathlib import Path

postprocess_rule_env = os.environ.copy()
postprocess_rule_env["SCRIPT_OUTPUT_FILE_COUNT"] = "1"
postprocess_rule_env["SCRIPT_OUTPUT_FILE_LIST_COUNT"] = "0"

excluded_patterns = list(map(Path, os.environ.get("EXCLUDED_SOURCE_FILE_NAMES", "").split()))
included_patterns = list(map(Path, os.environ.get("INCLUDED_SOURCE_FILE_NAMES", "").split()))

migrate_rule = os.environ["SCRIPT_INPUT_FILE_1"]


def pattern_match(path, pattern):
    # Using the number of path components in `pattern`, only match that many
    # trailing components in `path`. This is an imitation of Xcode's path
    # filtering behavior.
    components_to_match = zip(reversed(path.parts), reversed(pattern.parts))
    return all(fnmatch(path_component, pattern_component) for path_component, pattern_component in components_to_match)


for file_number in range(int(os.environ["SCRIPT_OUTPUT_FILE_LIST_COUNT"])):
    input_file_list = open(os.environ[f"SCRIPT_INPUT_FILE_LIST_{file_number}"])
    output_file_list = open(os.environ[f"SCRIPT_OUTPUT_FILE_LIST_{file_number}"])
    for input_file, output_file in zip(input_file_list, output_file_list):
        input_file = Path(input_file.rstrip())
        output_file = Path(output_file.rstrip())

        if input_file.name != output_file.name:
            sys.exit(f"error: Trying to migrate {input_file} to {output_file}, but the file names don't match. "
                     "Headers should always have the same name when migrated. "
                     "Ensure that the paths in MigrateHeaders-input.xcfilelist match the order in MigrateHeaders-output.xcfilelist.")

        if any(pattern_match(input_file, pattern) for pattern in excluded_patterns) \
                and not any(pattern_match(input_file, pattern) for pattern in included_patterns):
            continue

        postprocess_rule_env["INPUT_FILE_PATH"] = input_file
        postprocess_rule_env["INPUT_FILE_NAME"] = input_file.name
        postprocess_rule_env["SCRIPT_OUTPUT_FILE_0"] = output_file
        postprocess_rule_env["SCRIPT_HEADER_VISIBILITY"] = "private" if "PrivateHeaders" in output_file.parts else "public"
        print("RuleScriptExecution", output_file, input_file, file=sys.stderr)
        subprocess.check_call((migrate_rule), env=postprocess_rule_env)
