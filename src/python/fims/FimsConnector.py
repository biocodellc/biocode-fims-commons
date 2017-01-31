# -*- coding: utf-8 -*-


"""fims.FimsConnector: main class for communicating with FIMS REST services."""

import sys
import requests


class FimsConnector:
    authentication_url = "authenticationService/login"
    validate_url = "validate"
    upload_url = "validate/continue"

    def __init__(self, rest_root):
        self.rest_root = rest_root
        self.session = requests.Session()

    def authenticate(self, username, password):
        r = self.session.post(self.rest_root + self.authentication_url, data={
            'username': username,
            'password': password
        })

        if r.status_code > 299:
            print('status code: %s' % r.status_code)
            print(r.json()['usrMessage'] or 'Server Error')
            sys.exit()

    def validate(self, project_id, fims_metadata, expedition_code, upload, is_public):
        r = self.session.post(self.rest_root + self.validate_url,
                              files={
                                  'fimsMetadata': (fims_metadata, open(fims_metadata, 'rb'))
                              },
                              data={
                                  'upload': upload,
                                  'projectId': project_id,
                                  'expeditionCode': expedition_code,
                                  'public': is_public
                              })

        if r.status_code > 299:
            print('status code: %s' % r.status_code)
            print(r.json()['usrMessage'] or 'Server Error')
            sys.exit()

        return r.json()

    def upload(self, create_expedition=False):
        r = self.session.get(self.rest_root + self.upload_url + "?createExpedition=%s" % create_expedition)

        if r.status_code > 299:
            print('status code: %s' % r.status_code)
            print(r.json()['usrMessage'] or 'Server Error')
            sys.exit()

        return r.json()
