#!/bin/bash
# NOTE: Assumes $CATALINA_HOME and $GGS_DATA are set

GGS_HOME="${0%/*}"

### Available CPUs for container
CPUs=1 
### Overrite it with user's input, if user set it
if [[ ! -z "$POD_CPU_LIMIT" ]] && (($(echo $POD_CPU_LIMIT '>' 0|bc))); then
      CPUs=$POD_CPU_LIMIT
 
fi

### Using ceiling value of [ CPUs * 1.5 ]
### For less than 1 CPU, we are creating single instance of geostan
maxPoolActive=`echo "($CPUs * 1.5) + 0.5" | bc`
### Convert decimal to integer
maxPoolActive=${maxPoolActive%.*}  

### Just to make sure that we are getting at least one geostan instance
if [[ -z "$maxPoolActive" ]] || (($(echo $maxPoolActive '==' 0|bc))); then
  maxPoolActive=1
fi

if [[ ! -z "$GGS_POOL_MAX_ACTIVE" ]] && (( $GGS_POOL_MAX_ACTIVE > 0 )); then
	   maxPoolActive=$GGS_POOL_MAX_ACTIVE 
fi

remote="false"

if [[ "${GGS_REMOTE,,}" == "true" ]]; then
    remote="true"
fi

(cd $GGS_HOME/cli; sh ./cli.sh configure --s $GGS_DATA --d $GGS_HOME/resources/config)
(cd $GGS_HOME/cli; sh ./cli.sh setting --d $GGS_HOME/resources/config --t USA --n REMOTE --v $remote)
(cd $GGS_HOME/cli; sh ./cli.sh setting --d $GGS_HOME/resources/config --t USA --n POOL_MAX_ACTIVE --v $maxPoolActive)
 echo "Deploying the Geocoding application with settings: [REMOTE:$remote, POOL_MAX_ACTIVE:$maxPoolActive]"

# Need to start tomcat because we overrode the tomcat startup CMD in our dockerfile
catalina.sh run