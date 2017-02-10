import argparse
import hashlib
import methods

if __name__ == '__main__':
    parser = argparse.ArgumentParser('Script to delete a user via Thunder')

    # Add command line args
    parser.add_argument('username', type=str,
                        help='username of the user to delete')
    parser.add_argument('password', type=str,
                        help='password of the user to delete')
    parser.add_argument('-e', '--endpoint', type=str, default='http://localhost:8080',
                        help='the base endpoint to connect to')
    parser.add_argument('-v', '--verbosity', type=int, default=0, choices={0, 1},
                        help='0 = only success/failure. 1 = show HTTP response')
    parser.add_argument('-a', '--auth', type=str, default='application:secret',
                        help='authentication credentials to connect to the endpoint')
    args = parser.parse_args()

    # Hash password
    m = hashlib.md5()
    m.update(args.password.encode('utf-8'))
    password = m.hexdigest()

    # Separate auth
    auth = (args.auth.split(':')[0], args.auth.split(':')[1])

    # Make request
    methods.delete_user(args.endpoint + '/users',
                        authentication=auth,
                        params={'username': args.username},
                        headers={'password': password},
                        verbosity=args.verbosity)
