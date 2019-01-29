#!/bin/bash -xe
#
#  remote_init.sh -- remotely launch server init script
#

PROJECT="cantolang-site"
PROJECT_REPO_URL="https://github.com/CantoLang/cantolang-site.git"

ssh-keygen -f ~/.ssh/known_hosts -R ${HOST_ADDRESS}
ssh -t -t -o StrictHostKeyChecking=no root@$HOST_ADDRESS <<-HERE
    exec ssh-agent /bin/bash
    ssh-add /root/.ssh/$IDENTITY
    cd /opt
    rm -rf $PROJECT
    git clone $PROJECT_REPO_URL
    cd $PROJECT
    chmod +x install.sh
    . install.sh
    exit
HERE


