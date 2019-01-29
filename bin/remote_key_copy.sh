#!/bin/bash
#
#  remote_key_copy.sh -- copy an ssh key to a remote server
#

PRIVATE=0

while [[ $1 = -* ]]; do
    case $1 in
      -p) PRIVATE=1 ;;
      --private) PRIVATE=1 ;;
    esac
    shift
done

if [ $PRIVATE = 1 ]
then
    echo Copying PRIVATE key $IDENTITY to $HOST_ADDRESS...
    echo 
    rsync -avze ssh $KEY_PATH/$IDENTITY root@$HOST_ADDRESS:/root/.ssh
    rsync -avze ssh $KEY_PATH/$IDENTITY.pub root@$HOST_ADDRESS:/root/.ssh
    
    #!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!#
    # NOTE: the leading whitespace in the heredoc MUST BE TABS #
    #!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!#
    ssh -t -t root@$HOST_ADDRESS <<-HERE
		chmod 600 /root/.ssh/$IDENTITY
		exit
	HERE
else
    echo Copying public key $IDENTITY to $HOST_ADDRESS...
    echo 
    cat $KEY_PATH/$IDENTITY.pub | ssh root@$HOST_ADDRESS "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
fi
