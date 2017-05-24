# simple script to export mysql data to be loaded into postgres
import argparse, os, subprocess, time

TABLES = ["bcids", "expedition_bcids", "expeditions", "oauth_clients", "oauth_nonces", "oauth_tokens", "projects",
          "template_configs", "user_projects", "users"]

MYSQL_EXPORT_COLUMNS = "mysql -u {} -p{} -e \"SELECT GROUP_CONCAT(COLUMN_NAME SEPARATOR ',') FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema='{}' and table_name='{}' INTO OUTFILE '{}' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY " \
                       "'' ESCAPED BY '' LINES TERMINATED BY '\n';\""
MYSQL_DUMP = "mysqldump --single-transaction -u {} -p{} -T {} {} {} --fields-enclosed-by=\\\" --fields-terminated-by=,"


def dump(db, mysql_user, mysql_pass, output_dir):
    for table in TABLES:
        out_file = os.path.join(output_dir, "{}_output.txt".format(table))

        subprocess.check_call(MYSQL_EXPORT_COLUMNS.format(mysql_user, mysql_pass, db, table, out_file), shell=True)

        subprocess.check_call(MYSQL_DUMP.format(mysql_user, mysql_pass, output_dir, db, table), shell=True)

        subprocess.check_call("cat {} >> {}".format(os.path.join(output_dir, "{}.txt".format(table)), out_file), shell=True)
        
        subprocess.check_call("rm -f {}".format(os.path.join(output_dir, "{}.*".format(table))), shell=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='NEON Parser')
    parser.add_argument('mysql_user', help='the mysql user')
    parser.add_argument('mysql_pass', help='password for mysql')
    parser.add_argument('mysql_db', help='the mysql db to migrate')
    parser.add_argument('output_dir', help='directory to dump the files to')

    args = parser.parse_args()
    db = args.mysql_db.strip()
    mysql_user = args.mysql_user.strip()
    mysql_pass = args.mysql_pass.strip()
    output_dir = args.output_dir.strip()

    dump(db, mysql_user, mysql_pass, output_dir)
