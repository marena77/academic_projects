function [LandMarksComputed, AllPosesComputed] = SLAMusingGTSAM(DetAll, K, TagSize)
	% For Input and Output specifications refer to the project pdf

	import gtsam.*
	% Refer to Factor Graphs and GTSAM Introduction
	% https://research.cc.gatech.edu/borg/sites/edu.borg/files/downloads/gtsam.pdf
	% and the examples in the library in the GTSAM toolkit. See folder
	% gtsam_toolbox/gtsam_examples

    % get first pose from assuming tag 10 world coords
    % calculate world coords for all other tags in frame
    % get pose for second frame from known tag world coords
    % calculate world coords for all tags in frame using updated pose
    % get pose for third frame from common tag world coords

    LandMarksComputed = 0;
    AllPosesComputed = 0;
    tag10 = DetAll{1}(19,:);
    
    x = [tag10(2),tag10(4),tag10(6),tag10(8);tag10(3),tag10(5),tag10(7),tag10(9)];
    X = [0,0+TagSize,0+TagSize,0;0,0,0+TagSize,0+TagSize;0,0,0,0];
    
    % calculating pose for frame 1
    H = est_homography(x(1,:)',x(2,:)',X(1,:)',X(2,:)');
    [R, T, T_] = getPose(H, K);
    imageToWorld = inv(K*[R(:,1) R(:,2) T]);
    estimatedWorldPoints = zeros(81, 9);
    estimatedPoses = struct([]);
    estimatedPoses(1).R = R;
    estimatedPoses(1).T = T;
    estimatedPoses(1).T_ = T_;
 
    % calculating world coordinates for all frame 1 tags
    for tag = 1:size(DetAll{1},1)
        tagRow = DetAll{1}(tag,:);
        imageCoords = [tagRow(2),tagRow(4),tagRow(6),tagRow(8);tagRow(3),tagRow(5),tagRow(7),tagRow(9);1,1,1,1];
        worldCoords = imageToWorld*imageCoords;
        for col = 1:size(worldCoords,2)
            worldCoords(1,col) = worldCoords(1,col)/worldCoords(3,col);
            worldCoords(2,col) = worldCoords(2,col)/worldCoords(3,col);
        end
        estimatedWorldPoints(tagRow(1),:) = [tagRow(1) worldCoords(1,1)  worldCoords(2,1) worldCoords(1,2) worldCoords(2,2) ... 
             worldCoords(1,3) worldCoords(2,3) worldCoords(1,4) worldCoords(2,4)];
    end
    
    % main loop of pre-gtsam
    for frame = 2:size(DetAll,2)
        tagImageCoords = DetAll{frame};
        % get new pose
        
        for tag = 1:size(tagImageCoords,1)
            c = estimatedWorldPoints(estimatedWorldPoints(:,1) == tagImageCoords(tag,1), :);
            if c
                tagImageCoords(tag,10:17) = c(2:9);
            end
        end
        tagImageCoords(tagImageCoords(:,12) == 0, :) = [];
        % Now tagImageCoords has 17 columns, consisting of tagID, 8 image
        % coordinates, and 8 world coordinates
        points = zeros(4,4*size(tagImageCoords,1));
        for r = 1:size(tagImageCoords,1)
            points(1,4*r-3:4*r) = [tagImageCoords(r,2) tagImageCoords(r,4) tagImageCoords(r,6) tagImageCoords(r,8)];
            points(2,4*r-3:4*r) = [tagImageCoords(r,3) tagImageCoords(r,5) tagImageCoords(r,7) tagImageCoords(r,9)];
            points(3,4*r-3:4*r) = [tagImageCoords(r,10) tagImageCoords(r,12) tagImageCoords(r,14) tagImageCoords(r,16)];
            points(4,4*r-3:4*r) = [tagImageCoords(r,11) tagImageCoords(r,13) tagImageCoords(r,15) tagImageCoords(r,17)];
        end
        % Now points is formatted for easy homography calculation
        H = est_homography(points(1,:)',points(2,:)',points(3,:)',points(4,:)');
        [R, T, T_] = getPose(H, K);
        imageToWorld = inv(K*[R(:,1) R(:,2) T]);
        estimatedPoses(frame).R = R;
        estimatedPoses(frame).T = T;
        estimatedPoses(frame).T_ = T_;

        % update estimate world coords
        for tag = 1:size(DetAll{frame},1)
            tagRow = DetAll{frame}(tag,:);
            imageCoords = [tagRow(2),tagRow(4),tagRow(6),tagRow(8);tagRow(3),tagRow(5),tagRow(7),tagRow(9);1,1,1,1];
            worldCoords = imageToWorld*imageCoords;
            for col = 1:size(worldCoords,2)
                worldCoords(1,col) = worldCoords(1,col)/worldCoords(3,col);
                worldCoords(2,col) = worldCoords(2,col)/worldCoords(3,col);
            end
            estimatedWorldPoints(tagRow(1),:) = [tagRow(1) worldCoords(1,1)  worldCoords(2,1) worldCoords(1,2) worldCoords(2,2) ... 
                 worldCoords(1,3) worldCoords(2,3) worldCoords(1,4) worldCoords(2,4)];
        end
    end
    % plots pose estimates
%     X = zeros(1, 2267);
%     Y = zeros(1, 2267);
%     Z = zeros(1, 2267);
%     for frame = 1:2267
%         X(1, frame) = estimatedPoses(frame).T_(1,1);
%         Y(1, frame) = estimatedPoses(frame).T_(2,1);
%         Z(1, frame) = estimatedPoses(frame).T_(3,1);
%     end
%     figure;
%     scatter3(X, Y, Z, 'filled');
    graph = NonlinearFactorGraph;
    measurementNoiseSigma = [0.1; 0.1];
    pointNoiseSigma = 0.1;
    poseNoiseSigmas = [0.001; 0.001; 0.001; 0.1; 0.1; 0.1];
    odomNoise = noiseModel.Diagonal.Sigmas([.1; .1; .1; .1; .1; .1]);
    measurementNoise = noiseModel.Diagonal.Sigmas(measurementNoiseSigma);
    poseNoise = noiseModel.Diagonal.Sigmas(poseNoiseSigmas);
    pointNoise = noiseModel.Isotropic.Sigma(3, pointNoiseSigma);
    graph.add(PriorFactorPoint3(symbol('a', 10), Point3(0, 0, 0), pointNoise));
    graph.add(PriorFactorPose3(symbol('x', 0), Pose3(Rot3(estimatedPoses(1).R), Point3(estimatedPoses(1).T_)), poseNoise))

    initialEstimate = Values;
    fx = K(1,1);
    s = K(1,2);
    u0 = K(1,3);
    fy = K(2,2);
    v0 = K(2,3);
    K_ = Cal3_S2(double(fx), double(fy), double(s), double(u0), double(v0));
    initialEstimate.insert(symbol('x', 0), Pose3(Rot3(estimatedPoses(1).R), Point3(estimatedPoses(1).T_)));

    for frame = 1:2267
        initialEstimate.insert(symbol('x',frame), Pose3(Rot3(estimatedPoses(frame).R), Point3(estimatedPoses(frame).T_)))
        graph.add(BetweenFactorPose3(symbol('x', frame-1), symbol('x',frame), Pose3(Rot3(eye(3)), Point3(zeros(3,1))), odomNoise));
        for tag = 1:size(DetAll{frame},1)
            tagRow = DetAll{frame}(tag,:);
            graph.add(GenericProjectionFactorCal3_S2(Point2(tagRow(2), tagRow(3)), measurementNoise, symbol('x',frame), symbol('a', tagRow(1)), K_));
            graph.add(GenericProjectionFactorCal3_S2(Point2(tagRow(4), tagRow(5)), measurementNoise, symbol('x',frame), symbol('b', tagRow(1)), K_));
            graph.add(GenericProjectionFactorCal3_S2(Point2(tagRow(6), tagRow(7)), measurementNoise, symbol('x',frame), symbol('c', tagRow(1)), K_));
            graph.add(GenericProjectionFactorCal3_S2(Point2(tagRow(8), tagRow(9)), measurementNoise, symbol('x',frame), symbol('d', tagRow(1)), K_));
        end
    end

    for row = 1:size(estimatedWorldPoints,1)
        if estimatedWorldPoints(row,1) ~= 0
            initialEstimate.insert(symbol('a', estimatedWorldPoints(row,1)), Point3(estimatedWorldPoints(row,2), estimatedWorldPoints(row,3),  0));
            initialEstimate.insert(symbol('b', estimatedWorldPoints(row,1)), Point3(estimatedWorldPoints(row,4), estimatedWorldPoints(row,5),  0));
            initialEstimate.insert(symbol('c', estimatedWorldPoints(row,1)), Point3(estimatedWorldPoints(row,6), estimatedWorldPoints(row,7),  0));
            initialEstimate.insert(symbol('d', estimatedWorldPoints(row,1)), Point3(estimatedWorldPoints(row,8), estimatedWorldPoints(row,9),  0));
        end
    end
    
    optimizer = LevenbergMarquardtOptimizer(graph, initialEstimate);
    result = optimizer.optimizeSafely();
    X = zeros(1, 2267);
    Y = zeros(1, 2267);
    Z = zeros(1, 2267);
    for frame = 1:2267
        t = result.at(symbol('x', frame)).translation();
        X(1,frame) = t.x();
        Y(1,frame) = t.y();
        Z(1,frame) = t.z();
    end
    figure;
    scatter3(X, Y, Z, 'filled');

end
function [R, T, T_] = getPose(H, camParams)
    k = inv(camParams)*H;
    r = [k(:,1) k(:,2) cross(k(:,1), k(:,2))];
    [u, ~, v] = svd(r);
    R = u*[1,0,0;0,1,0;0,0,det(u*v')]*v';
    T = k(:,3)/norm(k(:,1));
    T_ = -R'*T;
    T_(3,1) = abs(T_(3,1));
end
function H = est_homography(X,Y,x,y)
    % H = est_homography(X,Y,x,y)
    % Compute the homography matrix from source(x,y) to destination(X,Y)
    %
    %    X,Y are coordinates of destination points
    %    x,y are coordinates of source points
    %    X/Y/x/y , each is a vector of n*1, n>= 4
    %
    %    H is the homography output 3x3
    %   (X,Y, 1)^T ~ H (x, y, 1)^T
    
    A = zeros(length(x(:))*2,9);
    
    for i = 1:length(x(:))
     a = [x(i),y(i),1];
     b = [0 0 0];
     c = [X(i);Y(i)];
     d = -c*a;
     A((i-1)*2+1:(i-1)*2+2,1:9) = [[a b;b a] d];
    end
    
    [U, S, V] = svd(A);
    h = V(:,9);
    H = reshape(h,3,3)';
end
