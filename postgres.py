# simple script to load exported mysql data
import argparse, os, subprocess

TABLES = ["users", "projects", "expeditions", "bcids", "expedition_bcids", "oauth_clients", "oauth_nonces", "oauth_tokens",
          "template_configs", "user_projects"]

COL_NAMES = "head -1 {}"

PSQL_IMPORT = "psql -U {} -d {} -c \"\\copy {} ({}) from '{}' with delimiter as ',' csv HEADER NULL AS '\\N';" \
              "select setval('{}_id_seq', max(id)) from {};\""


def dump(db, psql_user, input_dir):
    for table in TABLES:
        in_file = os.path.join(input_dir, "{}_output.txt".format(table))

        cols = subprocess.check_output(COL_NAMES.format(in_file), shell=True).decode("utf-8")
        cols = cols.strip("\n")

        print(PSQL_IMPORT.format(psql_user, db, table, cols, in_file, table, table))
        subprocess.check_call(PSQL_IMPORT.format(psql_user, db, table, cols, in_file, table, table), shell=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='NEON Parser')
    parser.add_argument('psql_user', help='the postgres user')
    parser.add_argument('psql_db', help='the psql db to migrate')
    parser.add_argument('input_dir', help='directory of files to import')

    args = parser.parse_args()
    db = args.psql_db.strip()
    psql_user = args.psql_user.strip()
    input_dir = args.input_dir.strip()

    dump(db, psql_user, input_dir)