from google.oauth2 import id_token
from google.auth.transport import requests
import boto3

CLIENT_ID = "990471046455-g5s0mqhm6sm3b66ki7fle6n2ud0msim1.apps.googleusercontent.com"
ID_TOKEN_KEY = "idTokenGoogle"
USER_POOL_ID = "us-east-1_vLSsBubHd"

def lambda_handler(event, context):
    event['response']['autoConfirmUser'] = False

    # save email to verify if email in token matches the cognito email
    email = event['request']['userAttributes']['email'].casefold()

    if event['request']['validationData'] != None and ID_TOKEN_KEY in event['request']['validationData']:
        valid_token = verify_token(event['request']['validationData'][ID_TOKEN_KEY], email)
        if valid_token:
            if not email_already_verified(email):
                event['response']['autoConfirmUser'] = True
                event['response']['autoVerifyEmail'] = True
            else:
                raise Exception("Email is already associated with an account")

    # Return to Amazon Cognito
    return event

def verify_token(token, email):
    try:
        id_info = id_token.verify_oauth2_token(token, requests.Request(), CLIENT_ID)
        return id_info['email'].casefold() == email
    except ValueError:
        # Invalid token
        return False

def email_already_verified(email):
    client = boto3.client('cognito-idp')

    filter = "email = \"{0}\"".format(email)
    response = client.list_users(UserPoolId=USER_POOL_ID, Filter=filter)

    if response != None and 'Users' in response:
        for user in response['Users']:
            if 'UserStatus' in user and user['UserStatus'] == 'CONFIRMED':
                return True
    return False
