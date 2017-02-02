# -*- coding: utf-8 -*-


"""fims.Fims: provides entry point main()."""
from .FimsConnector import FimsConnector

__version__ = "0.1.0"

import sys, argparse


def __sanitize_url(url):
    """add a trailing slash to the url if not present"""
    if not url.endswith("/"):
        url += "/"
    return url


def run(rest_url, project_id, dataset, username, password, expedition_code, upload=False,
        create_expedition=False, is_public=False):
    sanitized_rest_url = __sanitize_url(rest_url)

    fims_connector = FimsConnector(sanitized_rest_url)

    if upload:
        fims_connector.authenticate(username, password)

    r_validate = fims_connector.validate(project_id, dataset, expedition_code, upload, is_public)

    if 'continue' not in r_validate:
        __print_messages(r_validate['done'])
    else:
        r_upload = None
        if r_validate['continue']['message'] == "continue":
            r_upload = fims_connector.upload()
        else:
            __print_messages(r_validate['continue'])

            continue_upload = input("Would you like to continue to upload?: ")

            if continue_upload.lower() in ['y', 'yes', 'continue']:
                r_upload = fims_connector.upload()

        if r_upload:
            if 'continue' in r_upload:
                if create_expedition:
                    r_upload = fims_connector.upload(True)
                else:
                    create_expedition_input = input("The expedition [%s] doesn't exist.\n\tWould you like to create it now?: " % expedition_code)
                    if create_expedition_input.lower() not in ['y', 'yes', 'continue']:
                        sys.exit()
                    r_upload = fims_connector.upload(True)

            if 'error' in r_upload:
                print(r_upload['error'])
            else:
                print(r_upload['done'])


def __print_upload_messages(messages):
    print(messages)


def __print_messages(messages):
    if 'config' in messages and messages['config']:
        print("Invalid Project Configuration.\n "
              "Please talk to your project administrator to fix the following error(s):\n\n")

        for group_message, messages_array in messages['config']:
            __print_sheet_messages(group_message, messages_array, "Error")

        print("\n")

    for sheet_name, sheet_messages in messages['worksheets'].items():
        level = "warnings"
        if 'errors' in sheet_messages:
            level = "errors"

        print("Validation results on %s worksheet.\n"
              "1 or more %s found. Must fix to continue.\n\n" % (sheet_name, level))

        for group_message, messages_array in sheet_messages['errors'].items():
            __print_sheet_messages(group_message, messages_array, "Error")
        for group_message, messages_array in sheet_messages['warnings'].items():
            __print_sheet_messages(group_message, messages_array, "Warning")

        print("\n")


def __print_sheet_messages(group_message, messages_array, prefix):
    print("\t%s: %s\n" % (prefix, group_message))

    for msg in messages_array:
        print("\t\t%s\n" % msg)


def main():
    parser = argparse.ArgumentParser(
        description="FIMS cmd line access.",
        epilog="As an alternative to the commandline, params can be placed in a file, one per line, and specified on the commandline like '%(prog)s @params.conf'.",
        fromfile_prefix_chars='@')
    parser.add_argument(
        "rest_service",
        help="location of the FIMS REST services",
        default="http://biscicol.org/biocode-fims/rest/v2/"
    )
    parser.add_argument(
        "project_id",
        help="project_id to validate/upload against",
    )
    parser.add_argument(
        "dataset",
        help="the dataset to validate/upload")
    parser.add_argument(
        "-u",
        "--upload",
        help="upload the dataset",
        dest="upload",
        action="store_true")
    parser.add_argument(
        "--public",
        help="set the expedition to public. defaults to false",
        dest="is_public",
        action="store_true")
    parser.add_argument(
        "-e",
        "--expedition",
        help="expedition_code to upload the dataset to. required if uploading.",
        default='',
        dest="expedition_code")
    parser.add_argument(
        "--create",
        help="if the expedition doesn't exist, create a new expedition.",
        action="store_true",
        dest="create_expedition")
    parser.add_argument(
        "-user",
        "--username",
        help="username",
        dest="username")
    parser.add_argument(
        "-pass",
        "--password",
        help="password",
        dest="password")
    args = parser.parse_args()

    if args.upload:
        if not args.expedition_code:
            parser.error('expedition_code is required when uploading')
        if (not args.username) or (not args.password):
            parser.error('username and password are required when uploading')

    run(args.rest_service, args.project_id, args.dataset, args.username, args.password, args.expedition_code,
        args.upload, args.create_expedition, args.is_public)
