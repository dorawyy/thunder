import argparse
import json
import methods

if __name__ == '__main__':
    parser = argparse.ArgumentParser('Script to add a user via Thunder')

    # Add command line args
    parser.add_argument('filename', type=str,
                        help='JSON file containing the user details')
    parser.add_argument('-e', '--endpoint', type=str, default='http://localhost:8080',
                        help='the base endpoint to connect to')
    parser.add_argument('-v', '--verbosity', type=int, default=0, choices={0, 1},
                        help='0 = only success/failure. 1 = show HTTP response')
    parser.add_argument('-a', '--auth', type=str, default='application:secret',
                        help='authentication credentials to connect to the endpoint')
    args = parser.parse_args()

    # Separate auth
    auth = (args.auth.split(':')[0], args.auth.split(':')[1])

    # Read JSON
    with open(args.filename) as f:
        data = json.load(f)

    # Make request
    methods.add_user(args.endpoint + '/users',
                     authentication=auth,
                     body=data,
                     verbosity=args.verbosity)

